package me.jameshunt.plinko.store.db

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver.Companion.IN_MEMORY
import me.jameshunt.sqldelight.MerkleDatabase
import java.nio.ByteBuffer

object MerkleDB {
    private val driver: SqlDriver = JdbcSqliteDriver(IN_MEMORY).apply {
        MerkleDatabase.Schema.create(this)
    }

    private val db = MerkleDatabase(driver)

    val values = ValuesDB(db.valuesQueries)
    val docCollection = DocumentAndCollectionDB(db.documentsAndCollectionsQueries)
}

fun Boolean.toByteArray(): ByteArray = when (this) {
    false -> byteArrayOf(0)
    true -> byteArrayOf(1)
}

fun ByteArray.toBoolean(): Boolean = when {
    this.contentEquals(byteArrayOf(0)) -> false
    this.contentEquals(byteArrayOf(1)) -> true
    else -> throw IllegalStateException("byteArray is not a boolean")
}

fun Double.toByteArray(): ByteArray {
    val doubleToLongBits = java.lang.Double.doubleToLongBits(this)
    return ByteBuffer.allocate(java.lang.Long.BYTES).putLong(doubleToLongBits).array()
}

fun ByteArray.toDouble(): Double = ByteBuffer.wrap(this).double

fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(java.lang.Integer.BYTES).putInt(this).array()
fun ByteArray.toInt(): Int = ByteBuffer.wrap(this).int

fun ByteArray.toUTF8String(): String = this.toString(Charsets.UTF_8)
