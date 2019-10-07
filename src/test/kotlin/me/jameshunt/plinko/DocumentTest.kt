package me.jameshunt.plinko

import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.store.Plinko
import org.junit.Assert
import org.junit.Test
import java.io.File

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

        Plinko.collection("test").document("doc1").run {
            setData(tree1)
            setData(tree2)
            setData(tree3)
            setData(tree4)

            val data = getData()
            ObjectMapper().writeValueAsString(data).let(::println)
            Assert.assertEquals(tree4, data)
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
}
