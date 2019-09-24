package me.jameshunt.plinko.merkle

object DiffParser {

    fun parseDiff(diff: Map<String, Any>): ValueInfo {
        return diff.parseObject()
    }

    private fun Map<String, Any>.parseObject(): ValueInfo {

        val hash = this["hash"] as Map<String, String>
        val from = hash["from"] as String
        val to = hash["to"] as String

        return when {
            this["type"] == "object" -> ValueInfo.Object(
                from = from,
                to = to,
                children = (this["children"] as List<Map<String, Any>>).parseObjectChildren()
            )
            this["type"] == "array" -> ValueInfo.Array(
                from = from,
                to = to,
                children = (this["children"] as List<Map<String, Any>>).parseArrayChildren()
            )
            else -> return ValueInfo.Value(from = from, to = to)
        }
    }

    private fun List<Map<String, Any>>.parseObjectChildren(): Map<KeyInfo, ValueInfo?> {
        return this.map { child ->
            val childKey = when (val hash = (child["key"] as? Map<String, Any>)?.get("hash")) {
                is String -> KeyInfo.KeySame(hash)
                is Map<*, *> -> {
                    hash as Map<String, String>
                    KeyInfo.KeyChanged(
                        from = hash["from"] as String,
                        to = hash["to"] as String
                    )
                }
                else -> throw IllegalStateException()
            }

            val childValue = (child["value"] as? Map<String, Any>)?.parseObject()
            childKey to childValue
        }.toMap()
    }

    private fun List<Map<String, Any>>.parseArrayChildren(): List<ValueInfo> {
        return this.map { child -> child.parseObject() }
    }

    sealed class KeyInfo {
        data class KeySame(val hash: String) : KeyInfo()
        data class KeyChanged(val from: String, val to: String) : KeyInfo()
    }

    sealed class ValueInfo {
        data class Value(val from: String, val to: String) : ValueInfo()
        data class Object(val from: String, val to: String, val children: Map<KeyInfo, ValueInfo?>) : ValueInfo()
        data class Array(val from: String, val to: String, val children: List<ValueInfo>) : ValueInfo()
    }

}
