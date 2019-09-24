package me.jameshunt.plinko.merkle

import org.apache.commons.codec.digest.DigestUtils

val emptyOrNull: String = DigestUtils.md5Hex(byteArrayOf())

interface Node {
    val hash: String
}

data class JObject(val keyValues: Map<JValue, Node>) : Node {
    override val hash: String = keyValues
        .map { (key, node) -> key.hash + node.hash }
        .joinToString()
        .let(DigestUtils::md5Hex)
}

data class JArray(val array: List<Node>) : Node {
    override val hash: String = array
        .joinToString { it.hash }
        .let(DigestUtils::md5Hex)
}

data class JValue(val value: Any?, private var _hash: String? = null) : Node {
    override val hash: String = _hash
        ?: (value?.let { DigestUtils.md5Hex(it.toString()) } ?: DigestUtils.md5Hex(byteArrayOf()))
            .also { _hash = it }

    val valueType: ValueType
        get() = when (value) {
            null -> ValueType.Null
            is Boolean -> ValueType.Boolean
            is String -> ValueType.String
            is Double -> ValueType.Double
            is Int -> ValueType.Int
            else -> throw IllegalStateException("invalid type: $this")
        }
}

enum class ValueType {
    String,
    Int,
    Double,
    Boolean,
    Null
}


fun Node.extractJValues(): List<JValue> {
    fun JArray.extractJValues(): List<JValue> = this.array
        .map { it.extractJValues() }
        .flatten()

    fun JObject.extractJValues(): List<JValue> = this.keyValues
        .mapValues { (key, value) -> key.extractJValues() + value.extractJValues() }
        .values
        .flatten()

    return when (this) {
        is JObject -> this.extractJValues()
        is JArray -> this.extractJValues()
        is JValue -> listOf(this)
        else -> throw IllegalStateException("invalid extract type: $this")
    }
}
