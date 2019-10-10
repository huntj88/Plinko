package me.jameshunt.plinko

import me.jameshunt.plinko.merkle.JArray
import me.jameshunt.plinko.merkle.JObject
import me.jameshunt.plinko.merkle.JValue
import org.junit.Assert
import org.junit.Test

class HashTest {

    @Test
    fun emptyObjectHash() {
        val expected = "a8cfde6331bd59eb2ac96f8911c4b666"
        Assert.assertEquals(expected, JObject(mapOf()).hash)
    }

    @Test
    fun emptyArrayHash() {
        val expected = "f1f713c9e000f5d3f280adbd124df4f5"
        Assert.assertEquals(expected, JArray(listOf()).hash)
    }

    @Test
    fun nullValueHash() {
        val expected = "d41d8cd98f00b204e9800998ecf8427e"
        Assert.assertEquals(expected, JValue(null).hash)
    }
}
