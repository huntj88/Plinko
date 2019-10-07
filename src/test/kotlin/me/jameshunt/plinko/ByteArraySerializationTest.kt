package me.jameshunt.plinko

import me.jameshunt.plinko.store.db.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ByteArraySerializationTest {

    @Test
    fun testBooleanByteArray() {
        assertEquals(true, true.toByteArray().toBoolean())
        assertEquals(false, false.toByteArray().toBoolean())
        assertNotEquals(false, true.toByteArray().toBoolean())
        assertNotEquals(true, false.toByteArray().toBoolean())
    }

    @Test
    fun testDoubleByteArray() {
        assertEquals(1.0, 1.0.toByteArray().toDouble(), 0.00001)
        assertEquals(10022.22212, 10022.22212.toByteArray().toDouble(), 0.00001)
    }

    @Test
    fun testIntByteArray() {
        assertEquals(1, 1.toByteArray().toInt())
        assertEquals(20000000, 20000000.toByteArray().toInt())
    }

    @Test
    fun testStringByteArray() {
        assertEquals("hello", "hello".toByteArray().toUTF8String())
        assertEquals("20000000", "20000000".toByteArray().toUTF8String())
    }
}
