package me.jameshunt.plinko.merkle

import me.jameshunt.plinko.store.Plinko
import me.jameshunt.plinko.store.domain.Commit

object DiffMerge {

    fun mergeMasterWith(newCommits: List<Commit>): List<Commit> {
        // new commits could be interwoven with existing commits


        // apply new commits at common ancestor
        // cherry-pick over commits newer than common ancestor from master to new branch

        val commonAncestorHash = newCommits.first().diff.commitHashFrom()

        val docId = newCommits.first().documentId
        val masterCommits = Plinko.merkleDB.docCollection.getIncludedDocumentCommits(documentId = docId)

        val existingCommits = masterCommits.takeLastWhile {
            it.diff.commitHashTo() != commonAncestorHash
        }
        existingCommits.forEach(::println)

        val sharedHistory = (masterCommits - existingCommits)
            .map { DiffParser.parseDiff(it.diff) }
            .fold(HashObject(nullValue, emptyMap())) { partialDocument, nextDiff ->
                DiffCommit.commit(partialDocument, nextDiff) as HashObject
            }

        return mergeHistoryOrderedByDate(
            sharedHistory = sharedHistory,
            existingCommits = existingCommits,
            remainingExisting = existingCommits,
            newCommits = newCommits,
            remainingNew = newCommits,
            mergedCommits = emptyList()
        )
    }

    private tailrec fun mergeHistoryOrderedByDate(
        sharedHistory: HashObject,
        existingCommits: List<Commit>,
        remainingExisting: List<Commit>,
        newCommits: List<Commit>,
        remainingNew: List<Commit>,
        mergedCommits: List<Commit>
    ): List<Commit> {
        val nextCommit = (remainingExisting + remainingNew).minBy { it.createdAt }!!
        val nextFromCommitHash = nextCommit.diff.commitHashFrom()

        val mergedHistoryHash = mergedCommits.lastOrNull()?.diff?.commitHashTo() ?: sharedHistory.hash

        val newMergeCommit = when (mergedHistoryHash == nextFromCommitHash) {
            true -> nextCommit
            false -> {
                // TODO apply next commit

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
                            println(it.diff)
                            Transformation(from = it.diff.commitHashFrom(), to = it.diff.commitHashTo())
                        }

                        transformationsFromExistingToShared.forEach(::println)
                        transformationsUpMergeBranch.forEach(::println)

                        createNewDiffFromTransformations(
                            nextCommit = nextCommit,
                            transformationsFromExistingToShared = transformationsFromExistingToShared,
                            transformationsUpMergeBranch = transformationsUpMergeBranch
                        )
                    }
                    docForNew.hash -> TODO()
                    else -> throw IllegalStateException()
                }

                // trace hash changes from next commit to sharedHistory hash, and up the other branch

                // create a copy of mergedHistory HashObject, and then manipulate it with the correct
                // hashes for what the document should look like after the commit is applied, the newMergedHistory.
                // yay hashing stuff

                // take a diff of mergedHistory -> newMergedHistory, create commit

                // next commit
            }
        }

        if (((remainingExisting + remainingNew) - nextCommit).isEmpty()) {
            return newMergeCommit?.let { mergedCommits + it } ?: mergedCommits
        }

        return mergeHistoryOrderedByDate(
            sharedHistory = sharedHistory,
            existingCommits = existingCommits,
            remainingExisting = remainingExisting - nextCommit,
            newCommits = newCommits,
            remainingNew = remainingNew - nextCommit,
            mergedCommits = newMergeCommit?.let { mergedCommits + it } ?: mergedCommits
        )
    }

    //TODO: does not work with child collections
    // for existing to merge
    private fun createNewDiffFromTransformations(
        nextCommit: Commit,
        transformationsFromExistingToShared: List<Transformation>,
        transformationsUpMergeBranch: List<Transformation>
    ): Commit? {
        // need to recursively check child collections for hash changes too

        val newHash = mapOf(
            "from" to transformationsUpMergeBranch.last().to,
            "to" to "temp" //TODO
        )
        return nextCommit.copy(
            diff = nextCommit.diff
                .toMutableMap()
                .apply { this["hash"] = newHash }
        )
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
