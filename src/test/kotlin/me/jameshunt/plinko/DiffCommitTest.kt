package me.jameshunt.plinko

import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.merkle.*
import org.junit.Assert
import org.junit.Test

class DiffCommitTest {

    @Test
    fun `no diff`() {
        val json = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(json, json).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(json, diff)

        Assert.assertEquals(json, actual)
    }

    @Test
    fun `add key-simpleValue to object`() {
        val before = "{}".toJsonHashObject()

        val expected = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `remove key-simpleValue from object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()
        val expected = "{}".toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change key in object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val expected = """
            {
              "susan": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change simple value to different simple value`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": "yep"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change simple value to object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": {
                "wow": true
              }
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change object to simple value`() {
        val before = """
            {
              "bob": {
                "wow": true
              }
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change simple value to array`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change simple value to array with values`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change array to simple value`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change array with value to simple value`() {
        val before = """
            {
              "bob": [
                "wow"
              ]
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change object to array`() {
        val before = """
            {
              "bob": {"sup":true}
            }
        """.toJsonHashObject()
        val expected = """
            {
              "bob": ["no"]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change array to object`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val expected = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `change array (with values) to object (with values)`() {
        val before = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()
        val expected = """
            {
              "bob": {"yep": true}
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `add object to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val expected = """
            {
              "bob": [{}]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `remove object from array`() {
        val before = """
            {
              "bob": [{}]
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `add array to object`() {
        val before = "{}".toJsonHashObject()

        val expected = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `remove array from object`() {
        val before = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()
        val expected = "{}".toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `add simple value to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": [
                "hello"
              ]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `remove simple value from array`() {
        val before = """
            {
              "bob": [
                "hello"
              ]
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `add array to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val expected = """
            {
              "bob": [[]]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `remove array from array`() {
        val before = """
            {
              "bob": [["wow"]]
            }
        """.toJsonHashObject()
        val expected = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `add object to object`() {
        val before = "{}".toJsonHashObject()

        val expected = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `remove object from object`() {
        val before = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val expected = "{}".toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, expected).let { DiffParser.parseDiff(it) }
        val actual = DiffCommit.commit(before, diff)

        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `nested object change`() {
        val before = """
            {
              "hello": "wow",
              "child": {
                "nope": "yep"
              }
            }
            """.trimIndent().toJsonHashObject()

        val after = """
            {
              "hello": "wow",
              "child": {
                "nope": "yep",
                "wowza": "thing"
              }
            }
        """.trimIndent().toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val actual = DiffCommit.commit(before, diff)

        println(ObjectMapper().writeValueAsString(diff))
        println(ObjectMapper().writeValueAsString(actual))
        Assert.assertEquals(after, actual)

    }

    @Test
    fun `nested array change`() {
        val before = """
            {
              "hello": "wow",
              "child": [
                {
                  "nope": "yep"
                }
              ]
            }
            """.trimIndent().toJsonHashObject()

        val after = """
            {
              "hello": "wow",
              "child": [
                {
                  "nope": {}
                }
              ]
            }
        """.trimIndent().toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val actual = DiffCommit.commit(before, diff)

        println(ObjectMapper().writeValueAsString(diff))
        println(ObjectMapper().writeValueAsString(actual))
        Assert.assertEquals(after, actual)

    }
}
