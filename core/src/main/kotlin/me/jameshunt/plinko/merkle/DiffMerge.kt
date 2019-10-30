package me.jameshunt.plinko.merkle

import me.jameshunt.plinko.merkle.DiffParser.KeyInfo
import me.jameshunt.plinko.merkle.DiffParser.ValueInfo
import me.jameshunt.plinko.store.domain.Commit

object DiffMerge {

    fun mergeMasterWith(masterCommits: List<Commit>, newCommits: List<Commit>): List<Commit> {
        val commonAncestorHash = newCommits.first().diff.commitHashFrom()

        val existingCommits = masterCommits.takeLastWhile {
            it.diff.commitHashTo() != commonAncestorHash
        }

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
        // TODO: shortcut to merged
        // TODO: commitsUpMergeBranch.lastOrNull()?.original == nextCommit


        val transformationsFromExistingToShared = commitsFromSharedToExisting
            .map { DiffParser.parseDiff(it.diff) }
            .foldRight(DiffParser.parseDiff(nextCommit.diff)) { previousDiff, newDiffSoFar ->
                newDiffSoFar.transformUsingPrevious(previousDiff)
            }

        val transformationsFromSharedToMerged = commitsUpMergeBranch
            .map { DiffParser.parseDiff(it.merged.diff) }
            .fold(transformationsFromExistingToShared) { newDiffSoFar, nextDiff ->
                newDiffSoFar.transformUsingNextMerged(nextDiff)
            }
            .let { (it as ValueInfo.Object).requestRehash() }

        val newMergedBranchWrongHashes = DiffCommit.commit(mergedBranch, transformationsFromSharedToMerged) as HashObject

        val newMergedBranch = newMergedBranchWrongHashes.rehashFromValues()
        val newDiff = DiffGenerator.getDiff(mergedBranch, newMergedBranch)

        return nextCommit.copy(diff = newDiff)
    }

    private fun ValueInfo.transformUsingNextMerged(nextMerged: ValueInfo): ValueInfo {
        return when (this) {
            is ValueInfo.Object -> {
                when (nextMerged) {
                    is ValueInfo.Object -> {
                        DiffParser.ValueInfo.Object(
                            nextMerged.from,
                            nextMerged.to,
                            children = this.children
                                .map { (key, value) ->
                                    val matchingMerged = nextMerged
                                        .children.entries
                                        .firstOrNull { (nextKey, _) ->
                                            when {
                                                key is KeyInfo.KeySame && key == nextKey -> true
                                                key is KeyInfo.KeySame
                                                        && nextKey is KeyInfo.KeyChanged
                                                        && key.hash == nextKey.from -> true

                                                key is KeyInfo.KeyChanged
                                                        && nextKey is KeyInfo.KeyChanged
                                                        && key.to == nextKey.from -> true

                                                key is KeyInfo.KeyChanged
                                                        && nextKey is KeyInfo.KeySame
                                                        && key.to == nextKey.hash -> true
                                                else -> false
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
            is ValueInfo.Value -> {
                when(nextMerged) {
                    is ValueInfo.Value -> nextMerged
                    is ValueInfo.ValueToObject -> {
                        TODO("change to objectToValue and delete object")
                    }
                    else -> TODO("$nextMerged")
                }
            }
            else -> TODO()
        }
    }

    private fun KeyInfo.transformUsingNextMerged(
        nextMerged: KeyInfo,
        value: ValueInfo?
    ): KeyInfo {
        return when (nextMerged) {
            is KeyInfo.KeySame -> nextMerged
            is KeyInfo.KeyChanged -> {
                return if (nextMerged.from != nullValue && value == null) {
                    DiffParser.KeyInfo.KeySame(nextMerged.to)
                } else {
                    nextMerged
                }
            }
        }
    }

    private fun ValueInfo.transformUsingPrevious(previous: ValueInfo): ValueInfo {
        // CAN I REALLY DO THIS??
        return this.transformUsingNextMerged(previous)
    }

    private fun ValueInfo.Object.requestRehash(): ValueInfo.Object {
        return ValueInfo.Object(
            from = this.to,
            to = "REHASH NEEDED",
            children = this.children.map { (key, value) ->
                val rehashNeededIfCollection = when (value) {
                    is ValueInfo.Object -> value.requestRehash()
                    is ValueInfo.ArrayToObject -> TODO()
                    is ValueInfo.ValueToObject -> DiffParser.ValueInfo.Object(
                        from = value.from,
                        to = value.to,
                        children = value.objectChildren
                    ).requestRehash().let {
                        DiffParser.ValueInfo.ValueToObject(
                            from = it.from,
                            to = it.to,
                            objectChildren = it.children
                        )
                    }

                    is ValueInfo.Array -> TODO()
                    is ValueInfo.ObjectToArray -> TODO()
                    is ValueInfo.ValueToArray -> TODO()

                    is ValueInfo.Value -> value
                    is ValueInfo.ArrayToValue -> value
                    is ValueInfo.ObjectToValue -> value
                    null -> null
                }
                key to rehashNeededIfCollection
            }.toMap()
        )
    }

    private fun Map<String, Any>.commitHashTo(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["to"]!! }

    private fun Map<String, Any>.commitHashFrom(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["from"]!! }
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

