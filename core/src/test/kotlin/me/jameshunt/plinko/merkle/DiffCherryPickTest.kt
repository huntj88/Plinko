package me.jameshunt.plinko.merkle

import me.jameshunt.plinko.store.Plinko
import me.jameshunt.plinko.store.domain.Commit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DiffCherryPickTest {

    @Test
    fun testCherryPick() {
        val document = Plinko.collection("split").document("test1")

        val baseTime = OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

        val commit1 = Commit(
            documentId = document.data.id,
            createdAt = baseTime,
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf()).toHashObject(),
                JsonParser.read(mapOf("cool" to true)).toHashObject()
            )
        )

        // chronologically before 3, but not synced immediately
        // commit2ButSyncAfter3, should delete cool=true, but then commit 3 rebased on top
        val commit2ButSyncAfter3 = Commit(
            documentId = document.data.id,
            createdAt = baseTime.plusMinutes(2),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to true)).toHashObject(),
                JsonParser.read(mapOf()).toHashObject()
            )
        )

        val commit3 = Commit(
            documentId = document.data.id,
            createdAt = baseTime.plusMinutes(1),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf()).toHashObject(),
                JsonParser.read(mapOf("cool" to true, "wow" to null)).toHashObject()
            )
        )


        // commit 1 -> 3
        // commit 1 -> 2 cherry-pick 3

        val expected = mapOf("wow" to null)

    }

    @Test
    fun testCherryPickExploration() {
        Plinko.merkleDB.values.run {
            addJValue(JValue(null))
            addJValue(JValue("cool"))
            addJValue(JValue(true))
            addJValue(JValue("wow"))
        }

        val document = Plinko.collection("splitExplore").document("test1")

        val baseTime = OffsetDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

        val emptyDocument = JsonParser.read(mapOf()).toHashObject()

        val commit1 = Commit(
            documentId = document.data.id,
            createdAt = baseTime,
            diff = DiffGenerator.getDiff(
                emptyDocument,
                JsonParser.read(mapOf("cool" to true)).toHashObject()
            )
        )

        // chronologically before 3, but not synced immediately
        // commit2ButSyncAfter3, should delete cool=true, but then commit 3 rebased on top
        val commit2ButSyncAfter3 = Commit(
            documentId = document.data.id,
            createdAt = baseTime.plusMinutes(1),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to true)).toHashObject(),
                emptyDocument
            ).also { println("commit 2: ${Plinko.objectMapper.writeValueAsString(it)}") }
        )

        val commit3 = Commit(
            documentId = document.data.id,
            createdAt = baseTime.plusMinutes(2),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to true)).toHashObject(),
                JsonParser.read(mapOf("cool" to true, "wow" to null)).toHashObject()
            ).also { println("commit 3: ${Plinko.objectMapper.writeValueAsString(it)}") }
        )



        val commitExpected = Commit(
            documentId = document.data.id,
            createdAt = baseTime.plusMinutes(2),
            diff = DiffGenerator.getDiff(
                first = emptyDocument,
                second = JsonParser.read(mapOf("wow" to null)).toHashObject()
            ).also { println("commit expected: ${Plinko.objectMapper.writeValueAsString(it)}") }
        )

        val sharedHistory = DiffCommit.commit(emptyDocument, DiffParser.parseDiff(commit1.diff))

        val oldMasterDocument = DiffCommit.commit(
            sharedHistory as HashObject,
            DiffParser.parseDiff(commit3.diff)
        ).also {
            it as HashObject
            println("old: ${it.toJObject()}")
        }


        val newMasterPartial = DiffCommit.commit(
            sharedHistory as HashObject,
            DiffParser.parseDiff(commit2ButSyncAfter3.diff)
        ).also {
            it as HashObject
            println("new: ${it.toJObject()}")
        }


        val oldMasterSoftCommit = DiffGenerator.getDiff(
            oldMasterDocument as HashObject,
            newMasterPartial as HashObject
        ).also { println(Plinko.objectMapper.writeValueAsString(it)) }

        val oldMasterSoft = DiffCommit.commit(
            oldMasterDocument as HashObject,
            DiffParser.parseDiff(oldMasterSoftCommit)
        ).also {
            it as HashObject
            println("old master soft: ${it.toJObject()}")
        }


        val test = DiffGenerator.getDiff(oldMasterSoft as HashObject, sharedHistory)
            .also { println(Plinko.objectMapper.writeValueAsString(it)) }



        // commit 1 -> 3
        // commit 1 -> 2 cherry-pick 3

        val expected = mapOf("wow" to null)

    }

    @Test
    fun testSameHash() {

        val empty = JsonParser.read(mapOf()).toHashObject()
        val complete = JsonParser.read(mapOf("cool" to true, "wow" to null)).toHashObject()
        val diff1 = DiffGenerator.getDiff(
            empty,
            complete
        )

        val partial1 = JsonParser.read(mapOf("cool" to true)).toHashObject()
        val diff2 = DiffGenerator.getDiff(
            empty,
            partial1
        )

        val diff3 = DiffGenerator.getDiff(
            partial1,
            complete
        )

        val firstCommit = DiffCommit.commit(empty, DiffParser.parseDiff(diff1))
        val fromPartialsCommit = DiffCommit.commit(empty, DiffParser.parseDiff(diff2))
        val completeFromPartial = DiffCommit.commit(fromPartialsCommit as HashObject, DiffParser.parseDiff(diff3))

        assertEquals(firstCommit, completeFromPartial)


    }

    @Test
    fun takeLast() {
        listOf(1,2,3).takeLastWhile { it != 1 }.let(::println)
    }

    @Test
    fun blah() {
        val document = Plinko.collection("cherry-pick-test").document("test1")
        // commit 1
        document.setData(mapOf("cool" to true))

        val commit2ButSyncAfter3 = Commit(
            documentId = document.data.id,
            createdAt = OffsetDateTime.now(),
            diff = DiffGenerator.getDiff(
                JsonParser.read(mapOf("cool" to true)).toHashObject(),
                JsonParser.read(mapOf()).toHashObject()
            ).also { println("commit 2: ${Plinko.objectMapper.writeValueAsString(it)}") }
        )

        // commit 3
        document.setData(mapOf("cool" to true, "wow" to null))

        val docId = document.data.id
        DiffCherryPick.cherryPickFromMaster(listOf(commit2ButSyncAfter3))
    }
}
