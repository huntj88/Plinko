package me.jameshunt.plinko

import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.store.Plinko
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.time.OffsetDateTime

class DocumentTest {

    @Test
    fun `test get document JObject`() {
        val tree1 = File("src/test/resources/1.json")
            .readText()
            .toJson()

        val tree2 = File("src/test/resources/2.json")
            .readText()
            .toJson()

        val tree3 = File("src/test/resources/3.json")
            .readText()
            .toJson()

        val tree4 = File("src/test/resources/4.json")
            .readText()
            .toJson()

        val tree5 = File("src/test/resources/5.json")
            .readText()
            .toJson()

        val tree6 = File("src/test/resources/6.json")
            .readText()
            .toJson()

        val tree7 = File("src/test/resources/7.json")
            .readText()
            .toJson()

        val tree8 = File("src/test/resources/8.json")
            .readText()
            .toJson()

        val tree9 = File("src/test/resources/9.json")
            .readText()
            .toJson()

        val tree10 = File("src/test/resources/10.json")
            .readText()
            .toJson()

        val tree11 = File("src/test/resources/11.json")
            .readText()
            .toJson()

        Plinko.collection("test").document("doc1").run {
            setData(tree1)
            setData(tree2)
            setData(tree3)
            setData(tree4)
            setData(tree5)
            setData(tree6)
            setData(tree7)
            setData(tree8)
            setData(tree9)
            setData(tree10)
            setData(tree11)

            val data = getData()
            Assert.assertEquals(tree11, data)
        }
    }

    @Test
    fun `test document add child collection`() {

        val tree4 = File("src/test/resources/4.json")
            .readText()
            .toJson()

        Plinko.collection("testAddChildCollection").document("doc1").run {
            setData(tree4)

            val data = getData()
            ObjectMapper().writeValueAsString(data).let(::println)
            Assert.assertEquals(tree4, data)

            collection("childCollectionName").document("wow").run {
                setData(mapOf("wowza" to true))
            }
        }

        val docChildCollectionChildDocName = Plinko
            .collection("testAddChildCollection")
            .document("doc1")
            .collection("childCollectionName")
            .document("wow")
            .data.document_name

        Assert.assertEquals("wow", docChildCollectionChildDocName)
    }

    @Test
    fun `test asOfDate for document content`() {
        val collection = Plinko.collection("asOfDate")

        val testData: Map<String, Any?> = mapOf(
            "blah" to "hello"
        )

        val testData2: Map<String, Any?> = mapOf(
            "blah" to "wow"
        )

        val document = collection.document("test")
        document.setData(testData)

        val dateToQueryBy = OffsetDateTime.now()
        document.setData(testData2)

        Assert.assertEquals("hello", document.getData(asOfDate = dateToQueryBy)["blah"])
        Assert.assertEquals("wow", document.getData(asOfDate = OffsetDateTime.now())["blah"])
    }
}
