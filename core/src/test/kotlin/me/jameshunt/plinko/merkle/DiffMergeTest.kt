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
    fun `test cherry-pick 3 commits up existing branch when new is first`() {
        val commit1 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = HashObject(nullValue, emptyMap()),
                second = JsonParser.read(mapOf("cool" to true)).toHashObject()
            )
        )

        val commit2ButSyncAfter5 = Commit(
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

        val commit5 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = JsonParser.read(mapOf("cool" to true, "wow" to "not null")).toHashObject(),
                second = JsonParser.read(
                    mapOf(
                        "cool" to true,
                        "wow" to "not null",
                        "child" to mapOf(
                            "wow" to null
                        )
                    )
                ).toHashObject()
            )
        )

        val newMasterCommits = DiffMerge.mergeMasterWith(
            masterCommits = listOf(commit1, commit3, commit4, commit5).also { println(it) },
            newCommits = listOf(commit2ButSyncAfter5).also { println(it) }
        )

        val initialDocument = HashObject(nullValue, emptyMap())
        val newMasterBranch = newMasterCommits.fold(initialDocument) { partialDocument, c ->
            DiffCommit.commit(partialDocument, DiffParser.parseDiff(c.diff)) as HashObject
        }

        val expected = JsonParser.read(
            mapOf(
                "wow" to "not null",
                "child" to mapOf(
                    "wow" to null
                )
            )
        ).toHashObject()

        println(newMasterBranch)
        Assert.assertEquals(expected, newMasterBranch)
    }

    @Test
    fun `test adding to child object merge`() {
        val commit1 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = HashObject(nullValue, emptyMap()),
                second = JsonParser.read(mapOf("cool" to mapOf<String, Any>())).toHashObject()
            )
        )

        val commit2ButSyncAfter3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to mapOf<String, Any>())).toHashObject(),
                JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject()
            )
        )

        val commit3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = JsonParser.read(mapOf("cool" to mapOf<String, Any>())).toHashObject(),
                second = JsonParser.read(mapOf("cool" to mapOf("sup" to "dog"))).toHashObject()
            )
        )

        val newMasterCommits = DiffMerge.mergeMasterWith(
            masterCommits = listOf(commit1, commit3).also { println(it) },
            newCommits = listOf(commit2ButSyncAfter3).also { println(it) }
        )

        val initialDocument = HashObject(nullValue, emptyMap())
        val newMasterBranch = newMasterCommits.fold(initialDocument) { partialDocument, c ->
            DiffCommit.commit(partialDocument, DiffParser.parseDiff(c.diff)) as HashObject
        }

        val expected = JsonParser.read(
            mapOf(
                "cool" to mapOf(
                    "hey" to "there",
                    "sup" to "dog"
                )
            )
        ).toHashObject()

        println(newMasterBranch)
        Assert.assertEquals(expected, newMasterBranch)
    }


    @Test
    fun `test changing key`() {
        val commit1 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = HashObject(nullValue, emptyMap()),
                second = JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject()
            )
        )

        val commit2ButSyncAfter3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject(),
                JsonParser.read(mapOf("cool" to mapOf("sup" to "there"))).toHashObject()
            )
        )

        val commit3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject(),
                second = JsonParser.read(mapOf("cool" to mapOf("hey" to "there dog"))).toHashObject()
            )
        )

        val expected = JsonParser.read(
            mapOf(
                "cool" to mapOf(
                    "sup" to "there dog"
                )
            )
        ).toHashObject()

        println("expected:  $expected")

        val newMasterCommits = DiffMerge.mergeMasterWith(
            masterCommits = listOf(commit1, commit3).also { println(it) },
            newCommits = listOf(commit2ButSyncAfter3).also { println(it) }
        )

        val initialDocument = HashObject(nullValue, emptyMap())
        val newMasterBranch = newMasterCommits.fold(initialDocument) { partialDocument, c ->
            DiffCommit.commit(partialDocument, DiffParser.parseDiff(c.diff)) as HashObject
        }

        println(newMasterBranch)
        Assert.assertEquals(expected, newMasterBranch)
    }

    @Test
    fun `test changing value to object`() {
        val commit1 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = HashObject(nullValue, emptyMap()),
                second = JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject()
            )
        )

        val commit2ButSyncAfter3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject(),
                JsonParser.read(mapOf("cool" to mapOf("hey" to mapOf<String, String>()))).toHashObject()
            )
        )

        val commit3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject(),
                second = JsonParser.read(mapOf("cool" to mapOf("hey" to "there", "sup" to "there dog"))).toHashObject()
            )
        )

        val expected = JsonParser.read(
            mapOf(
                "cool" to mapOf(
                    "hey" to mapOf<String, String>(),
                    "sup" to "there dog"
                )
            )
        ).toHashObject()

        println("expected:  $expected")

        val newMasterCommits = DiffMerge.mergeMasterWith(
            masterCommits = listOf(commit1, commit3).also { println(it) },
            newCommits = listOf(commit2ButSyncAfter3).also { println(it) }
        )

        val initialDocument = HashObject(nullValue, emptyMap())
        val newMasterBranch = newMasterCommits.fold(initialDocument) { partialDocument, c ->
            DiffCommit.commit(partialDocument, DiffParser.parseDiff(c.diff)) as HashObject
        }

        println(newMasterBranch)
        Assert.assertEquals(expected, newMasterBranch)
    }

    @Test
    fun `override object with value`() {
        val commit1 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = HashObject(nullValue, emptyMap()),
                second = JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject()
            )
        )

        val commit2ButSyncAfter3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject(),
                JsonParser.read(mapOf("cool" to mapOf("hey" to mapOf("delete" to "the object i'm in")))).toHashObject()
            )
        )

        val commit3 = Commit(
            documentId = 20,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                first = JsonParser.read(mapOf("cool" to mapOf("hey" to "there"))).toHashObject(),
                second = JsonParser.read(mapOf("cool" to mapOf("hey" to "there dog"))).toHashObject()
            )
        )

        val expected = JsonParser.read(
            mapOf(
                "cool" to mapOf(
                    "hey" to "there dog"
                )
            )
        ).toHashObject()

        println("expected:  $expected")

        val newMasterCommits = DiffMerge.mergeMasterWith(
            masterCommits = listOf(commit1, commit3).also { println(it) },
            newCommits = listOf(commit2ButSyncAfter3).also { println(it) }
        )

        val initialDocument = HashObject(nullValue, emptyMap())
        val newMasterBranch = newMasterCommits.fold(initialDocument) { partialDocument, c ->
            DiffCommit.commit(partialDocument, DiffParser.parseDiff(c.diff)) as HashObject
        }

        println(newMasterBranch)
        Assert.assertEquals(expected, newMasterBranch)
    }
}
