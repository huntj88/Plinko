package me.jameshunt.plinko.merkle

import me.jameshunt.plinko.store.domain.Commit
import org.junit.Assert
import org.junit.Test
import java.time.OffsetDateTime

class DiffMergeTest {

    @Test
    fun `test delete field during merge`() {
        val commit1 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = HashObject(nullValue, emptyMap()),
                second = JsonParser.read(mapOf("cool" to true)).toHashObject()
            )
        )

        val commit2ButSyncAfter3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to true)).toHashObject(),
                JsonParser.read(mapOf()).toHashObject()
            )
        )

        val commit3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = JsonParser.read(mapOf("cool" to true)).toHashObject(),
                second = JsonParser.read(mapOf("cool" to true, "wow" to null)).toHashObject()
            )
        )

        val newMasterCommits = DiffMerge.mergeMasterWith(
            masterCommits = listOf(commit1, commit3),
            newCommits = listOf(commit2ButSyncAfter3)
        )

        val initialDocument = HashObject(nullValue, emptyMap())
        val newMasterBranch = newMasterCommits.fold(initialDocument) { partialDocument, c ->
            DiffCommit.commit(partialDocument, DiffParser.parseDiff(c.diff)) as HashObject
        }

        val expected = JsonParser.read(mapOf("wow" to null)).toHashObject()

        Assert.assertEquals(expected, newMasterBranch)
    }

    @Test
    fun `test second cherry-pick up existing branch when new is first`() {
        val commit1 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = HashObject(nullValue, emptyMap()),
                second = JsonParser.read(mapOf("cool" to true)).toHashObject()
            )
        )

        val commit2ButSyncAfter4 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to true)).toHashObject(),
                JsonParser.read(mapOf()).toHashObject()
            )
        )

        val commit3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = JsonParser.read(mapOf("cool" to true)).toHashObject(),
                second = JsonParser.read(mapOf("cool" to true, "wow" to null)).toHashObject()
            )
        )

        val commit4 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = JsonParser.read(mapOf("cool" to true, "wow" to null)).toHashObject(),
                second = JsonParser.read(mapOf("cool" to true, "wow" to "not null")).toHashObject()
            )
        )

        val newMasterCommits = DiffMerge.mergeMasterWith(
            masterCommits = listOf(commit1, commit3, commit4).also { println(it) },
            newCommits = listOf(commit2ButSyncAfter4).also { println(it) }
        )

        val initialDocument = HashObject(nullValue, emptyMap())
        val newMasterBranch = newMasterCommits.fold(initialDocument) { partialDocument, c ->
            DiffCommit.commit(partialDocument, DiffParser.parseDiff(c.diff)) as HashObject
        }

        val expected = JsonParser.read(mapOf("wow" to "not null")).toHashObject()

        println(newMasterBranch)
        Assert.assertEquals(expected, newMasterBranch)
    }
}
