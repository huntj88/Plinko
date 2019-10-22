package me.jameshunt.plinko.merkle

import me.jameshunt.plinko.store.Plinko
import me.jameshunt.plinko.store.domain.Commit

object DiffCherryPick {

    fun cherryPickFromMaster(newCommits: List<Commit>) {
        // new commits could be interwoven with existing commits


        // apply new commits at common ancestor
        // cherry-pick over commits newer than common ancestor from master to new branch

        val commonAncestorHash = newCommits.first().diff.fromCommitHash()

        val docId = newCommits.first().documentId
        val masterCommits = Plinko.merkleDB.docCollection.getIncludedDocumentCommits(documentId = docId)

        val existingCommits = masterCommits.takeLastWhile {
            it.diff.commitHash() != commonAncestorHash
        }
        existingCommits.forEach(::println)

        val sharedHistory = (masterCommits - existingCommits)
            .map { DiffParser.parseDiff(it.diff) }
            .fold(HashObject(nullValue, emptyMap())) { partialDocument, nextDiff ->
                DiffCommit.commit(partialDocument, nextDiff) as HashObject
            }

        val newMasterBranchCommits = mergeHistoryOrderedByDate(
            sharedHistory = sharedHistory,
            mergedHistory = sharedHistory,
            existingCommits = existingCommits,
            remainingExisting = existingCommits,
            newCommits = newCommits,
            remainingNew = newCommits
        )
    }

    private tailrec fun mergeHistoryOrderedByDate(
        sharedHistory: HashObject,
        mergedHistory: HashObject,
        existingCommits: List<Commit>,
        remainingExisting: List<Commit>,
        newCommits: List<Commit>,
        remainingNew: List<Commit>
    ): List<Commit> {
        val nextCommit = (remainingExisting + remainingNew).minBy { it.createdAt }!!
        val nextFromCommitHash = nextCommit.diff.fromCommitHash()
        val mergedHistoryAfterCommit = when (mergedHistory.hash == nextFromCommitHash) {
            true -> DiffCommit.commit(sharedHistory, DiffParser.parseDiff(nextCommit.diff)) as HashObject
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
                    docForExisting.hash -> TODO() // go from
                    docForNew.hash -> TODO()
                    else -> throw IllegalStateException()
                }

                // trace hash changes from next commit to sharedHistory hash, and up the other branch

                // create a copy of mergedHistory HashObject, and then manipulate it with the correct
                // hashes for what the document should look like after the commit is applied, the newMergedHistory.
                // yay hashing stuff

                // take a diff of mergedHistory -> newMergedHistory, create commit

                // next commit

                nextCommit

                TODO()
            }
        }

        return mergeHistoryOrderedByDate(
            sharedHistory = sharedHistory,
            mergedHistory = mergedHistoryAfterCommit,
            existingCommits = existingCommits,
            remainingExisting = remainingExisting - nextCommit,
            newCommits = newCommits,
            remainingNew = remainingNew - nextCommit
        )
    }

    private fun Map<String, Any>.commitHash(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["to"]!! }

    private fun Map<String, Any>.fromCommitHash(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["from"]!! }
}
