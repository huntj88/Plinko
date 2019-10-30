package me.jameshunt.plinko.merkle

import me.jameshunt.plinko.store.domain.Commit

object DiffMerge {

    fun mergeMasterWith(masterCommits: List<Commit>, newCommits: List<Commit>): List<Commit> {
        val commonAncestorHash = newCommits.first().diff.commitHashFrom()

        val existingCommits = masterCommits.takeLastWhile {
            it.diff.commitHashTo() != commonAncestorHash
        }
        existingCommits.forEach(::println)

        val sharedCommits = masterCommits - existingCommits
        val sharedHistory = sharedCommits
            .map { DiffParser.parseDiff(it.diff) }
            .fold(HashObject(nullValue, emptyMap())) { partialDocument, nextDiff ->
                DiffCommit.commit(partialDocument, nextDiff) as HashObject
            }

        return sharedCommits + mergeHistoryOrderedByDate(
            sharedHistory = sharedHistory,
            existingCommits = existingCommits,
            remainingExisting = existingCommits,
            newCommits = newCommits,
            remainingNew = newCommits,
            mergedCommits = emptyList()
        )
    }

    data class MergedAndOriginal(
        val merged: Commit,
        val original: Commit
    )

    private tailrec fun mergeHistoryOrderedByDate(
        sharedHistory: HashObject,
        existingCommits: List<Commit>,
        remainingExisting: List<Commit>,
        newCommits: List<Commit>,
        remainingNew: List<Commit>,
        mergedCommits: List<MergedAndOriginal>
    ): List<Commit> {
        val nextCommit = (remainingNew + remainingExisting).minBy { it.createdAt }!!
        val nextFromCommitHash = nextCommit.diff.commitHashFrom()

        val mergedHistoryHash = mergedCommits.lastOrNull()?.merged?.diff?.commitHashTo() ?: sharedHistory.hash

        val newMergeCommit = when (mergedHistoryHash == nextFromCommitHash) {
            true -> nextCommit
            false -> {
                val alreadyAppliedExistingCommits = (existingCommits - remainingExisting)
                val docForExisting = alreadyAppliedExistingCommits.fold(sharedHistory) { partialDoc, c ->
                    DiffCommit.commit(partialDoc, c.diff.let(DiffParser::parseDiff)) as HashObject
                }

                val alreadyAppliedNewCommits = (newCommits - remainingNew)
                val docForNew = alreadyAppliedNewCommits.fold(sharedHistory) { partialDoc, c ->
                    DiffCommit.commit(partialDoc, c.diff.let(DiffParser::parseDiff)) as HashObject
                }

                when (nextFromCommitHash) {
                    docForExisting.hash -> createNewCommitFromExisting(
                        mergedBranch = mergedCommits
                            .map { it.merged }
                            .fold(sharedHistory) { partialDoc, c ->
                                DiffCommit.commit(partialDoc, DiffParser.parseDiff(c.diff)) as HashObject
                            },
                        nextCommit = nextCommit,
                        commitsFromSharedToExisting = alreadyAppliedExistingCommits,
                        commitsUpMergeBranch = mergedCommits
                    )
                    docForNew.hash -> TODO()
                    else -> throw IllegalStateException()
                }
            }
        }

        if (((remainingExisting + remainingNew) - nextCommit).isEmpty()) {
            val existingMerged = mergedCommits.map { it.merged }
            return newMergeCommit?.let { existingMerged + it } ?: existingMerged
        }

        return mergeHistoryOrderedByDate(
            sharedHistory = sharedHistory,
            existingCommits = existingCommits,
            remainingExisting = remainingExisting - nextCommit,
            newCommits = newCommits,
            remainingNew = remainingNew - nextCommit,
            mergedCommits = newMergeCommit?.let {
                mergedCommits + MergedAndOriginal(merged = it, original = nextCommit)
            } ?: mergedCommits
        )
    }

    private fun createNewCommitFromExisting(
        mergedBranch: HashObject,
        nextCommit: Commit,
        commitsFromSharedToExisting: List<Commit>,
        commitsUpMergeBranch: List<MergedAndOriginal>
    ): Commit? {
        // navigate through existing to shared
        // check and see if each commit already has a merged variant, jump to it

        // otherwise, keep going.

        // TODO: shortcut to merged
        // TODO: commitsUpMergeBranch.lastOrNull()?.original == nextCommit


        val transformationsFromExistingToShared = commitsFromSharedToExisting
            .map { DiffParser.parseDiff(it.diff) }
            .foldRight(DiffParser.parseDiff(nextCommit.diff)) { previousDiff, newDiffSoFar ->

                newDiffSoFar.transformUsingPrevious(previousDiff)
            }

        println(transformationsFromExistingToShared)

        val transformationsFromSharedToMerged = commitsUpMergeBranch
            .map { DiffParser.parseDiff(it.merged.diff) }
            .fold(transformationsFromExistingToShared) { newDiffSoFar, nextDiff ->
                newDiffSoFar.transformUsingNextMerged(nextDiff)
            }
            .let { (it as DiffParser.ValueInfo.Object).requestRehash() }

        val newMergedBranchWrongHashes =
            DiffCommit.commit(mergedBranch, transformationsFromSharedToMerged) as HashObject

        println(transformationsFromSharedToMerged)
        println(newMergedBranchWrongHashes)

        val newMergedBranch = newMergedBranchWrongHashes.rehashFromValues()
        val newDiff = DiffGenerator.getDiff(mergedBranch, newMergedBranch)

        return nextCommit.copy(diff = newDiff)
    }

    private fun DiffParser.ValueInfo.Object.requestRehash(): DiffParser.ValueInfo.Object {
        return copy(
            from = this.to,
            to = "REHASH NEEDED",
            children = this.children.map { (key, value) ->
                val rehashNeededIfCollection = when (value) {
                    is DiffParser.ValueInfo.Object -> value.requestRehash()
                    is DiffParser.ValueInfo.ArrayToObject -> TODO()
                    is DiffParser.ValueInfo.ValueToObject -> TODO()

                    is DiffParser.ValueInfo.Array -> TODO()
                    is DiffParser.ValueInfo.ObjectToArray -> TODO()
                    is DiffParser.ValueInfo.ValueToArray -> TODO()

                    is DiffParser.ValueInfo.Value -> value
                    is DiffParser.ValueInfo.ArrayToValue -> value
                    is DiffParser.ValueInfo.ObjectToValue -> value
                    null -> null
                }
                key to rehashNeededIfCollection
            }.toMap()
        )
    }

    private fun DiffParser.ValueInfo.transformUsingNextMerged(nextMerged: DiffParser.ValueInfo): DiffParser.ValueInfo {
        println()
        println("this: $this")
        println("merged: $nextMerged")
        println()
        return when (this) {
            is DiffParser.ValueInfo.Object -> {
                when (nextMerged) {
                    is DiffParser.ValueInfo.Object -> {
                        DiffParser.ValueInfo.Object(
                            nextMerged.from,
                            nextMerged.to,
                            children = this.children
                                .map { (key, value) ->
                                    val matchingMerged = nextMerged
                                        .children.entries
                                        .firstOrNull { (nextKey, nextValue) ->

                                            // TODO: refactor
                                            if (key is DiffParser.KeyInfo.KeySame && key == nextKey) {
                                                true
                                            } else if (key is DiffParser.KeyInfo.KeySame
                                                && nextKey is DiffParser.KeyInfo.KeyChanged
                                                && key.hash == nextKey.from
                                            ) {
                                                true
                                            } else if (key is DiffParser.KeyInfo.KeyChanged
                                                && nextKey is DiffParser.KeyInfo.KeyChanged
                                                && key.to == nextKey.from
                                            ) {
                                                true
                                            } else if (false) {
                                                TODO()
                                            } else {
                                                false
                                            }
                                        }

                                    when (matchingMerged == null) {
                                        true -> key to value
                                        false -> {
                                            val keyTransformed =
                                                key.transformUsingNextMerged(matchingMerged.key, matchingMerged.value)
                                            val valueTransformed =
                                                value?.transformUsingNextMerged(matchingMerged.value ?: value)

                                            keyTransformed to valueTransformed
                                        }
                                    }
                                }.toMap()
                        )
                    }
                    else -> TODO()
                }
            }
            is DiffParser.ValueInfo.Value -> {
                if (this == nextMerged) {
                    return this
                } else {
//                    TODO()
                    nextMerged
                }
            }
            else -> TODO()
        }
    }

    private fun DiffParser.KeyInfo.transformUsingNextMerged(
        nextMerged: DiffParser.KeyInfo,
        value: DiffParser.ValueInfo?
    ): DiffParser.KeyInfo {

        return when (nextMerged) {
            is DiffParser.KeyInfo.KeySame -> nextMerged
            is DiffParser.KeyInfo.KeyChanged -> {
                return if (nextMerged.from != nullValue && value == null) {
                    DiffParser.KeyInfo.KeySame(nextMerged.to)
                } else {
                    nextMerged
                }
            }
        }
    }

    // TODO: tail recurse through children?
    private fun DiffParser.ValueInfo.transformUsingPrevious(previous: DiffParser.ValueInfo): DiffParser.ValueInfo {
//        when (this) {
//            is DiffParser.ValueInfo.Object -> {
//                when (previous) {
//                    is DiffParser.ValueInfo.Object -> {
//                        DiffParser.ValueInfo.Object(
//                            previous.from,
//                            previous.to,
//                            children = this.children.map { (key, value) ->
//                                TODO()
//                            }.toMap()
//                        )
//                    }
//                    else -> TODO()
//                }
//            }
//            else -> TODO()
//        }

        TODO()
    }

    private fun Map<String, Any>.commitHashTo(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["to"]!! }

    private fun Map<String, Any>.commitHashFrom(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["from"]!! }

    data class Transformation(
        val from: String,
        val to: String,
        val children: List<Transformation>
    )
}


private fun HashObject.rehashFromValues(): HashObject {
    // TODO: does not go down children
    val correctedChildren = hObject.map { (keyHash, value) ->
        keyHash to when (value) {
            is HashObject -> value.rehashFromValues()
            is HashArray -> TODO()
            is HashValue -> HashValue(value.hash)
            else -> throw IllegalStateException()
        }
    }.toMap()

    val correctedHash = correctedChildren.map { (keyHash, valueNode) ->
        keyHash to valueNode.hash
    }.toMap().hashForObjectType()

    return copy(hash = correctedHash, hObject = correctedChildren)
}

