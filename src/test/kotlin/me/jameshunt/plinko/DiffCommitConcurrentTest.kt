package me.jameshunt.plinko

import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.merkle.*
import org.junit.Assert
import org.junit.Test

class DiffCommitConcurrentTest {

    @Test
    fun `two diverging`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val diverge1 = """
            {
              "bob": "nope",
              "susan": null
            }
        """.toJsonHashObject()

        val diverge2 = """
            {
              "bob": "yep"
            }
        """.toJsonHashObject()

        val expected = """
            {
              "bob": "yep",
              "susan": null
            }
        """.toJsonHashObject()

        val diff1 = DiffGenerator.getDiff(before, diverge1).let { DiffParser.parseDiff(it) }
        val diff2 = DiffGenerator.getDiff(before, diverge2).let { DiffParser.parseDiff(it) }

        val mergedDiff = DiffMerge.merge(diff1, diff2).let { DiffParser.parseDiff(it) }

        println(ObjectMapper().writeValueAsString(mergedDiff))

        val actual = DiffCommit.commit(before, mergedDiff)

        Assert.assertEquals(expected, actual)
    }
}
