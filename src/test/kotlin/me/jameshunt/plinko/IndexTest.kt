package me.jameshunt.plinko

import me.jameshunt.plinko.store.Plinko
import me.jameshunt.plinko.store.db.MerkleDB
import me.jameshunt.plinko.store.domain.now
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Assert.*
import org.junit.Test
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class IndexTest {

    @Test
    fun `test set get index`() {
        Plinko.collection("indexTest").run {
            setIndex("wow.sup")
            val index = document("banana").getIndexedFields().first()

            println(index)
            assertEquals("wow.sup", index.key)
        }
    }

    @Test
    fun `test retrieve doc indexes`() {
        val testData: Map<String, Any?> = mapOf("index" to mapOf("test1" to true))

        val collection = Plinko.collection("indexTest")
        collection.setIndex("index.test1")

        val document = collection.document("indexTest1")
        document.setData(testData)

        val keyHash = DigestUtils.md5Hex("index.test1")

        val indexMatches = MerkleDB.docCollection.getDocumentIndex(collection.data.id, keyHash)

        indexMatches
            .firstOrNull { it.key_hash == keyHash }
            ?.also { println(it) }
            ?: fail()
    }

    @Test
    fun `test where equals using index`() {
        val collection = Plinko.collection("whereEquals")
        collection.setIndex("index.testKey")

        val testData1: Map<String, Any?> = mapOf("index" to mapOf("testKey" to true))
        val document1 = collection.document("indexTest1")
        document1.setData(testData1)

        val testData2: Map<String, Any?> = mapOf("index" to mapOf("testKey" to false))
        val document2 = collection.document("indexTest2")
        document2.setData(testData2)

        val testData3: Map<String, Any?> = mapOf("index" to mapOf("blah" to false))
        val document3 = collection.document("indexTest3")
        document3.setData(testData3)

        val testData4: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to "says hello",
                "testKey" to true
            )
        )
        val document4 = collection.document("indexTest4")
        document4.setData(testData4)

        val results = collection.query {
            whereEqualTo("index.testKey", true)
        }

        val docIds = results.map { it.data.id }

        assertTrue(docIds.contains(document1.data.id))
        assertTrue(docIds.contains(document4.data.id))

        assertFalse(docIds.contains(document2.data.id))
        assertFalse(docIds.contains(document3.data.id))
    }

    @Test
    fun `test query and`() {
        val collection = Plinko.collection("andTest")
        collection.setIndex("index.testKey")
        collection.setIndex("index.monkey")

        val testData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to "says hello",
                "testKey" to true
            )
        )
        val document = collection.document("andDocTest")
        document.setData(testData)

        val missingKeyData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "testKey" to true
            )
        )
        val missingKeyDoc = collection.document("missingKey")
        missingKeyDoc.setData(missingKeyData)

        val results = collection.query {
            whereEqualTo("index.testKey", true) and whereEqualTo("index.monkey", "says hello")
        }

        assertTrue(results.map { it.data.id }.contains(document.data.id))
        assertFalse(results.map { it.data.id }.contains(missingKeyDoc.data.id))

    }

    @Test
    fun `test query or`() {
        val collection = Plinko.collection("orTest")
        collection.setIndex("index.testKey")
        collection.setIndex("index.monkey")

        val testData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to "says hello",
                "testKey" to false
            )
        )
        val document = collection.document("andDocTest")
        document.setData(testData)

        val missingKeyData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "testKey" to true
            )
        )
        val missingKeyDoc = collection.document("missingKey")
        missingKeyDoc.setData(missingKeyData)

        val results = collection.query {
            whereEqualTo("index.testKey", true) or whereEqualTo("index.monkey", "says hello")
        }

        assertTrue(results.map { it.data.id }.contains(document.data.id))
        assertTrue(results.map { it.data.id }.contains(missingKeyDoc.data.id))
    }

    @Test
    fun `test compound query (and)or(and)`() {
        val collection = Plinko.collection("andOrAnd")
        collection.setIndex("index.testKey")
        collection.setIndex("index.monkey")

        val testData1: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to "says hello",
                "testKey" to true
            )
        )
        val document1 = collection.document("indexTest1")
        document1.setData(testData1)

        val testData2: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to "hello",
                "testKey" to false
            )
        )
        val document2 = collection.document("indexTest2")
        document2.setData(testData2)

        val testData3: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to "says hello",
                "testKey" to false
            )
        )
        val document3 = collection.document("indexTest3")
        document3.setData(testData3)

        val results = collection.query {
            val query1 = whereEqualTo("index.testKey", true) and whereEqualTo("index.monkey", "says hello")
            val query2 = whereEqualTo("index.testKey", false) and whereEqualTo("index.monkey", "hello")

            query1 or query2
        }

        assertTrue(results.map { it.data.id }.contains(document1.data.id))
        assertTrue(results.map { it.data.id }.contains(document2.data.id))
        assertFalse(results.map { it.data.id }.contains(document3.data.id))
    }

    @Test
    fun `test update index`() {
        val collection = Plinko.collection("indexUpdateTest1")
        collection.setIndex("index.monkey")

        val testData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to 5
            )
        )
        val document = collection.document("andDocTest")
        document.setData(testData)

        val changedValueData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to "something DIFFERENT"
            )
        )
        document.setData(changedValueData)

        val expectNoResults = collection.query {
            whereEqualTo("index.monkey", 5)
        }

        val expectResults = collection.query {
            whereEqualTo("index.monkey", "something DIFFERENT")
        }

        println(expectNoResults)
        println(expectResults)

        assertFalse("result should be empty", expectNoResults.map { it.data.id }.contains(document.data.id))
        assertTrue("one result", expectResults.map { it.data.id }.contains(document.data.id))
    }

    @Test
    fun `test whereGreaterThan`() {
        val collection = Plinko.collection("whereGreaterThan")
        collection.setIndex("index.monkey")

        val testData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to 5
            )
        )
        val document = collection.document("greaterThan1")
        document.setData(testData)

        val testData2: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to 10.2
            )
        )
        val document2 = collection.document("greaterThan2")
        document2.setData(testData2)

        val results = collection.query {
            whereGreaterThan("index.monkey", 5.1)
        }

        assertFalse(results.map { it.data.id }.contains(document.data.id))
        assertTrue(results.map { it.data.id }.contains(document2.data.id))
    }

    @Test
    fun `test whereLessThan`() {
        val collection = Plinko.collection("whereLessThan")
        collection.setIndex("index.monkey")

        val testData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to 5
            )
        )
        val document = collection.document("greaterThan1")
        document.setData(testData)

        val testData2: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to 10.2
            )
        )
        val document2 = collection.document("greaterThan2")
        document2.setData(testData2)

        val results = collection.query {
            whereLessThan("index.monkey", 5.1)
        }

        assertTrue(results.map { it.data.id }.contains(document.data.id))
        assertFalse(results.map { it.data.id }.contains(document2.data.id))
    }

    @Test
    fun `test whereLessThan or whereGreaterThan`() {
        val collection = Plinko.collection("whereLessGreaterThan")
        collection.setIndex("index.monkey")

        val testData: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to 5
            )
        )
        val document = collection.document("greaterThan1")
        document.setData(testData)

        val testData2: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to 10.2
            )
        )
        val document2 = collection.document("greaterThan2")
        document2.setData(testData2)

        val testData3: Map<String, Any?> = mapOf(
            "index" to mapOf(
                "monkey" to 7.5
            )
        )
        val document3 = collection.document("greaterThan3")
        document3.setData(testData3)

        val results = collection.query {
            whereLessThan("index.monkey", 6) or whereGreaterThan("index.monkey", 9.2)
        }

        assertTrue(results.map { it.data.id }.contains(document.data.id))
        assertTrue(results.map { it.data.id }.contains(document2.data.id))
        assertFalse(results.map { it.data.id }.contains(document3.data.id))
    }

    @Test
    fun `test Where date`() {
        val collection = Plinko.collection("whereDate")
        collection.setIndex("time")

        val testData: Map<String, Any?> = mapOf(
            "time" to OffsetDateTime.now()
                .plusDays(1)
                .format(DateTimeFormatter.ISO_INSTANT)
        )
        val document = collection.document("dateFuture")
        document.setData(testData)

        val testData2: Map<String, Any?> = mapOf(
            "time" to OffsetDateTime.now()
                .minusDays(1)
                .format(DateTimeFormatter.ISO_INSTANT)
        )
        val document2 = collection.document("datePast")
        document2.setData(testData2)

        val results = collection.query {
            whereLessThan("time", now)
        }

        assertFalse(results.map { it.data.id }.contains(document.data.id))
        assertTrue(results.map { it.data.id }.contains(document2.data.id))
    }
}
