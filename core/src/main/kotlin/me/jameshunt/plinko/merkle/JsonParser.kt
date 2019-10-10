package me.jameshunt.plinko.merkle

object JsonParser {

    fun read(json: Map<String, Any?>): JObject {
        return json
            .map { (key, value) -> key.toNode() as JValue to value.toNode() }
            .let { JObject(it.toMap()) }
    }

    private fun Any?.toNode(): Node {
        fun Map<String, Any?>.handleMap(): Node = this
            .map { (key, value) -> key.toNode() as JValue to value.toNode() }
            .let { JObject(it.toMap()) }

        fun List<Any?>.handleList(): Node = this
            .map { it.toNode() }
            .let { JArray(it) }

        return when (this) {
            null -> JValue(null)
            is String, is Int, is Boolean, is Double -> JValue(this)
            is List<*> -> this.handleList()
            is Map<*, *> -> (this as Map<String, Any?>).handleMap()
            else -> throw IllegalStateException("invalid type: $this")
        }
    }

    fun write(jObject: JObject): Map<String, Any?> = jObject.keyValues
        .map {
            val key = it.key.value as String
            key to when (val node = it.value) {
                is JObject -> write(node)
                is JArray -> write(node)
                is JValue -> node.value
                else -> throw IllegalStateException()
            }
        }
        .toMap()

    private fun write(jArray: JArray): List<Any?> = jArray.array.map {
        when (it) {
            is JObject -> write(it)
            is JArray -> write(it)
            is JValue -> it.value
            else -> throw IllegalStateException()
        }
    }
}
