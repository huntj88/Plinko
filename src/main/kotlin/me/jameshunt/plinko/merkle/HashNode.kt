package me.jameshunt.plinko.merkle

interface HashNode {
    val hash: String
}

data class HashObject(
    override val hash: String,
    val hObject: Map<String, HashNode>
) : HashNode

data class HashArray(
    override val hash: String,
    val hArray: List<HashNode>
) : HashNode

data class HashValue(
    override val hash: String
) : HashNode


fun JObject.toHashObject(): HashObject = this.keyValues
    .map { (key, value) ->
        val hashNode: HashNode = when (value) {
            is JObject -> value.toHashObject()
            is JArray -> value.toHashArray()
            is JValue -> value.toHashValue()
            else -> throw IllegalStateException()
        }
        key.hash to hashNode
    }
    .toMap()
    .let { HashObject(this.hash, it) }

private fun JArray.toHashArray(): HashArray = this.array
    .map { value ->
        when (value) {
            is JObject -> value.toHashObject()
            is JArray -> value.toHashArray()
            is JValue -> value.toHashValue()
            else -> throw IllegalStateException()
        }
    }
    .let { HashArray(this.hash, it) }

private fun JValue.toHashValue(): HashValue = HashValue(this.hash)


private fun HashObject.getValueHashes(): List<String> {
    return this.hObject.map { (key, node) ->
        listOf(key) + when(node) {
            is HashObject -> node.getValueHashes()
            is HashArray -> node.getValueHashes()
            is HashValue -> listOf(node.hash)
            else -> throw IllegalStateException()
        }
    }.flatten()
}

private fun HashArray.getValueHashes(): List<String> {
    return this.hArray.map { node ->
        when(node) {
            is HashObject -> node.getValueHashes()
            is HashArray -> node.getValueHashes()
            is HashValue -> listOf(node.hash)
            else -> throw IllegalStateException()
        }
    }.flatten()
}

// TODO
//fun HashObject.toJObject() = this.toJObject(MerkleDB.values.getJValues(this.getValueHashes()))

private fun HashObject.toJObject(jValues: List<JValue>): JObject {
    fun List<JValue>.valueForHash(hash: String) = this.first { it.hash == hash }

    val keyValues = this.hObject.map { (hashKey, node) ->
        when(node) {
            is HashObject -> jValues.valueForHash(hashKey) to node.toJObject(jValues)
            is HashArray -> jValues.valueForHash(hashKey) to node.toJArray(jValues)
            is HashValue -> jValues.valueForHash(hashKey) to jValues.valueForHash(node.hash)
            else -> throw IllegalStateException()
        }
    }.toMap()

    return JObject(keyValues)
}

private fun HashArray.toJArray(jValues: List<JValue>): JArray {
    fun List<JValue>.valueForHash(hash: String) = this.first { it.hash == hash }

    val values = this.hArray.map { node ->
        when (node) {
            is HashObject -> node.toJObject(jValues)
            is HashArray -> node.toJArray(jValues)
            is HashValue -> jValues.valueForHash(node.hash)
            else -> throw IllegalStateException()
        }
    }

    return JArray(values)
}
