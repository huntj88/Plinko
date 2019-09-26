package me.jameshunt.plinko.merkle

object DiffParser {

    fun parseDiff(diff: Map<String, Any>): ValueInfo = diff.parseObject()

    private fun Map<String, Any>.parseObject(): ValueInfo {
        val hash = this["hash"] as Map<String, String>
        val from = hash["from"] as String
        val to = hash["to"] as String

        fun Any?.getToType(): String = (this as Map<String, String>)["to"]!!
        fun Any?.getFromType(): String = (this as Map<String, String>)["from"]!!

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
            this["type"] == "value" -> return ValueInfo.Value(from = from, to = to)
            this["type"].getFromType() == "object" -> {
                when (this["type"].getToType()) {
                    "array" -> DiffParser.ValueInfo.ObjectToArray(
                        from = from,
                        to = to,
                        objectChildren = (this["children"] as List<Map<String, Any>>).parseObjectChildren(),
                        arrayChildren = (this["children"] as List<Map<String, Any>>).parseArrayChildren()
                    )
                    "value" -> ValueInfo.ObjectToValue(
                        from = from,
                        to = to,
                        objectChildren = (this["children"] as List<Map<String, Any>>).parseObjectChildren()
                    )
                    else -> throw IllegalStateException()
                }
            }
            this["type"].getFromType() == "array" -> {
                when (this["type"].getToType()) {
                    "object" -> ValueInfo.ArrayToObject(
                        from = from,
                        to = to,
                        objectChildren = (this["children"] as List<Map<String, Any>>).parseObjectChildren(),
                        arrayChildren = (this["children"] as List<Map<String, Any>>).parseArrayChildren()
                    )
                    "value" -> ValueInfo.ArrayToValue(
                        from = from,
                        to = to,
                        arrayChildren = (this["children"] as List<Map<String, Any>>).parseArrayChildren()
                    )
                    else -> throw IllegalStateException()
                }
            }
            this["type"].getFromType() == "value" -> {
                when (this["type"].getToType()) {
                    "object" -> ValueInfo.ValueToObject(
                        from = from,
                        to = to,
                        objectChildren = (this["children"] as List<Map<String, Any>>).parseObjectChildren()
                    )
                    "array" -> ValueInfo.ValueToArray(
                        from = from,
                        to = to,
                        arrayChildren = (this["children"] as List<Map<String, Any>>).parseArrayChildren()
                    )
                    else -> throw IllegalStateException()
                }
            }
            else -> throw IllegalStateException()
        }
    }

    private fun List<Map<String, Any>>.parseObjectChildren(): Map<KeyInfo, ValueInfo?> {
        return this
            .filter { it.containsKey("key") }
            .map { child ->
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
            }
            .toMap()
    }

    private fun List<Map<String, Any>>.parseArrayChildren(): List<ValueInfo> {
        return this
            .filter { !it.containsKey("key") }
            .map { child -> child.parseObject() }
    }

    sealed class KeyInfo {
        data class KeySame(val hash: String) : KeyInfo()
        data class KeyChanged(val from: String, val to: String) : KeyInfo()
    }

    sealed class ValueInfo {
        data class Object(val from: String, val to: String, val children: Map<KeyInfo, ValueInfo?>) : ValueInfo()
        data class Array(val from: String, val to: String, val children: List<ValueInfo>) : ValueInfo()
        data class Value(val from: String, val to: String) : ValueInfo()

        data class ObjectToArray(
            val from: String,
            val to: String,
            val objectChildren: Map<KeyInfo, ValueInfo?>,
            val arrayChildren: List<ValueInfo>
        ) : ValueInfo()

        data class ObjectToValue(
            val from: String,
            val to: String,
            val objectChildren: Map<KeyInfo, ValueInfo?>
        ) : ValueInfo()

        data class ArrayToObject(
            val from: String,
            val to: String,
            val arrayChildren: List<ValueInfo>,
            val objectChildren: Map<KeyInfo, ValueInfo?>
        ) : ValueInfo()

        data class ArrayToValue(
            val from: String,
            val to: String,
            val arrayChildren: List<ValueInfo>
        ) : ValueInfo()

        data class ValueToObject(
            val from: String,
            val to: String,
            val objectChildren: Map<KeyInfo, ValueInfo?>
        ) : ValueInfo()

        data class ValueToArray(
            val from: String,
            val to: String,
            val arrayChildren: List<ValueInfo>
        ) : ValueInfo()
    }

}
