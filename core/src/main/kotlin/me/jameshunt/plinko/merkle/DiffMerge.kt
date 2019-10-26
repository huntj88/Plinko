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
                    docForExisting.hash -> {
                        val transformationsFromExistingToShared = (alreadyAppliedExistingCommits + nextCommit).map {
                            println(it.diff)
                            Transformation(from = it.diff.commitHashFrom(), to = it.diff.commitHashTo())
                        }

                        val transformationsUpMergeBranch = mergedCommits.map {
                            println(it.merged.diff)
                            Transformation(from = it.merged.diff.commitHashFrom(), to = it.merged.diff.commitHashTo())
                        }

                        transformationsFromExistingToShared.forEach(::println)
                        transformationsUpMergeBranch.forEach(::println)

                        createNewCommitFromTransformations(
                            mergedBranch = mergedCommits
                                .map { it.merged }
                                .fold(sharedHistory) { partialDoc, c ->
                                    DiffCommit.commit(partialDoc, DiffParser.parseDiff(c.diff)) as HashObject
                                },
                            nextCommit = nextCommit,
                            transformationsFromExistingToShared = transformationsFromExistingToShared,
                            transformationsUpMergeBranch = transformationsUpMergeBranch
                        )
                    }
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

    //TODO: does not work with child collections
    // for existing to merge
    private fun createNewCommitFromTransformations(
        mergedBranch: HashObject,
        nextCommit: Commit,
        transformationsFromExistingToShared: List<Transformation>,
        transformationsUpMergeBranch: List<Transformation>
    ): Commit? {
        // need to recursively check child collections for hash changes too

        val newHash = mapOf(
            "from" to transformationsUpMergeBranch.last().to,
            "to" to "temp" //TODO
        )

        val tempDiff = nextCommit.diff
            .toMutableMap()
            .apply { this["hash"] = newHash }

        val newMergedBranchWrongHashes = DiffCommit.commit(mergedBranch, DiffParser.parseDiff(tempDiff)) as HashObject
        val newMergedBranch = newMergedBranchWrongHashes.rehashFromValues()
        val newDiff = DiffGenerator.getDiff(mergedBranch, newMergedBranch)

        return nextCommit.copy(diff = newDiff)
    }

    private fun Map<String, Any>.commitHashTo(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["to"]!! }

    private fun Map<String, Any>.commitHashFrom(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["from"]!! }

    data class Transformation(
        val from: String,
        val to: String
    )
}


private fun HashObject.rehashFromValues(): HashObject {
    // TODO: does not go down children
    val correctedHash = hObject.map { it.key to it.value.hash }.toMap().hashForObjectType()
    return copy(hash = correctedHash)
}

