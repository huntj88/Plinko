package me.jameshunt.plinko.store.db

import me.jameshunt.db.ValuesQueries
import me.jameshunt.plinko.merkle.JValue
import me.jameshunt.plinko.merkle.ValueType

class ValuesDB(private val valuesQueries: ValuesQueries) {

    fun addJValue(jValue: JValue) {
        val (valueType, byteArray) = when (val value = jValue.value) {
            null -> ValueType.Null to byteArrayOf()
            is Boolean -> ValueType.Boolean to value.toByteArray()
            is String -> ValueType.String to value.toByteArray()
            is Double -> ValueType.Double to value.toByteArray()
            is Int -> ValueType.Int to value.toByteArray()
            else -> throw IllegalStateException("invalid type: $jValue")
        }

        valuesQueries.insert(jValue.hash, byteArray, valueType.toString())
    }

    fun getJValues(md5s: List<String>): List<JValue> {
        return valuesQueries.selectWhereMD5(md5s) { hash, valueByteArray, value_type ->
            val value: Any? = when (ValueType.valueOf(value_type)) {
                ValueType.String -> valueByteArray.toUTF8String()
                ValueType.Int -> valueByteArray.toInt()
                ValueType.Double -> valueByteArray.toDouble()
                ValueType.Boolean -> valueByteArray.toBoolean()
                ValueType.Null -> null
            }

            JValue(value, hash)
        }.executeAsList()
    }
}
