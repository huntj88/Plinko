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

        applyRemainingCommitsOrderedByDate(
            sharedHistory = sharedHistory,
            mergedHistory = sharedHistory,
            existingCommits = existingCommits,
            remainingExisting = existingCommits,
            newCommits = newCommits,
            remainingNew = newCommits
        )
    }

    private fun applyRemainingCommitsOrderedByDate(
        sharedHistory: HashObject,
        mergedHistory: HashObject,
        existingCommits: List<Commit>,
        remainingExisting: List<Commit>,
        newCommits: List<Commit>,
        remainingNew: List<Commit>
    ): HashObject {
        val nextCommit = (remainingExisting + remainingNew).minBy { it.createdAt }!!
        val mergedHistoryAfterCommit = when (mergedHistory.hash == nextCommit.diff.fromCommitHash()) {
            true -> DiffCommit.commit(sharedHistory, DiffParser.parseDiff(nextCommit.diff)) as HashObject
            false -> {
                // TODO apply next commit


                TODO()
            }
        }

        return applyRemainingCommitsOrderedByDate(
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
