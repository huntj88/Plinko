package me.jameshunt.plinko

import me.jameshunt.plinko.merkle.DiffGenerator
import me.jameshunt.plinko.merkle.DiffParser
import me.jameshunt.plinko.merkle.DiffRevert
import org.junit.Assert
import org.junit.Test

class DiffRevertTest {

    @Test
    fun `no diff`() {
        val json = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(json, json).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(json, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(json, actual)
    }

    @Test
    fun `add key-simpleValue to object`() {
        val before = "{}".toJsonHashObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `remove key-simpleValue from object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()
        val after = "{}".toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change key in object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "susan": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)
        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change simple value to different simple value`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": "yep"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change simple value to object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": {
                "wow": true
              }
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
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

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change simple value to array`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change simple value to array with values`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change array to simple value`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
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

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change object to array`() {
        val before = """
            {
              "bob": {"sup":true}
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": ["no"]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change array to object`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `change array (with values) to object (with values)`() {
        val before = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": {"yep": true}
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `add object to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": [{}]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `remove object from array`() {
        val before = """
            {
              "bob": [{}]
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `add array to object`() {
        val before = "{}".toJsonHashObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `remove array from object`() {
        val before = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()
        val after = "{}".toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `add simple value to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": [
                "hello"
              ]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
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

        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `add array to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": [[]]
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `remove array from array`() {
        val before = """
            {
              "bob": [["wow"]]
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `add object to object`() {
        val before = "{}".toJsonHashObject()

        val after = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }

    @Test
    fun `remove object from object`() {
        val before = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val after = "{}".toJsonHashObject()

        val diff = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        val actual = DiffRevert.revert(after, diff as DiffParser.ValueInfo.Object)

        Assert.assertEquals(before, actual)
    }
}
