package me.jameshunt.plinko.merkle

import me.jameshunt.plinko.store.Plinko
import me.jameshunt.plinko.store.domain.Commit

object DiffCherryPick {

    fun cherryPickFromMaster(newCommits: List<Commit>) {
        // apply new commits at common ancestor
        // cherry-pick over commits newer than common ancestor from master to new branch

        val commonAncestorHash = newCommits.first().diff.commitFromHash()

        val docId = newCommits.first().documentId
        val masterCommits = Plinko.merkleDB.docCollection.getIncludedDocumentCommits(documentId = docId)

        val divergentCommits = masterCommits.takeLastWhile { it.diff.commitHash() != commonAncestorHash }
        divergentCommits.forEach(::println)

        val sharedHistory = (masterCommits - divergentCommits)
            .map { DiffParser.parseDiff(it.diff) }
            .fold(HashObject(nullValue, emptyMap())) { partialDocument, nextDiff ->
                DiffCommit.commit(partialDocument, nextDiff) as HashObject
            }

        val newMasterBeforeCherryPick = newCommits
            .map { DiffParser.parseDiff(it.diff) }
            .fold(sharedHistory) { partialDocument, nextDiff ->
                DiffCommit.commit(partialDocument, nextDiff) as HashObject
            }

        newMasterBeforeCherryPick.also { println(Plinko.objectMapper.writeValueAsString(it)) }
    }

    private fun Map<String, Any>.commitHash(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["to"]!! }

    private fun Map<String, Any>.commitFromHash(): String = this["hash"]
        .let { it as Map<String, String> }
        .let { it["from"]!! }
}
