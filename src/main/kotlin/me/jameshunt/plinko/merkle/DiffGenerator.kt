package me.jameshunt.plinko.merkle

import com.fasterxml.jackson.databind.ObjectMapper

object DiffGenerator {

    fun getDiff(first: HashObject, second: HashNode): Map<String, Any> {
        when (second) {
            is HashObject -> {
                val unChanged = first.hObject.toList().filter { (keyA, nodeA) ->
                    second.hObject.toList()
                        .firstOrNull { (keyB, nodeB) -> keyA == keyB && nodeA.hash == nodeB.hash }
                        ?.let { (_, nodeB) -> nodeA == nodeB }
                        ?: false
                }

                println(unChanged)

                val changed = (first.hObject.toList() + second.hObject.toList())
                    .filter { !unChanged.contains(it) }

                println(changed)

                val keyChanged = changed.filter {
                    val firstContainsValue = first.hObject.containsValue(it.second)
                    val secondContainsValue = second.hObject.containsValue(it.second)
                    firstContainsValue && secondContainsValue
                }

                val valueChanged = changed.filter {
                    val firstContainsKey = first.hObject.containsKey(it.first)
                    val secondContainsKey = second.hObject.containsKey(it.first)
                    firstContainsKey && secondContainsKey
                }

                val added = second.hObject.toList().filter {
                    // TODO: do i need `!unChanged.contains(it)` after I bake type into hash?
                    !keyChanged.contains(it) && !valueChanged.contains(it) && !unChanged.contains(it)
                }

                val removed = (changed.subtract(keyChanged + valueChanged)) - added

                println("added: $added")
                println("removed: $removed")

                val addedDiff = added.map { (addedKey, addedNode) ->
                    val keyMap = mapOf("key" to hashChanged(from = nullValue, to = addedKey))
                    val valueChildren = when (addedNode) {
                        is HashObject -> getDiff(HashObject(nullValue, mapOf()), addedNode) + objectType()
                        is HashArray -> getDiff(HashArray(nullValue, listOf()), addedNode) + arrayType()
                        is HashValue -> hashChanged(from = nullValue, to = addedNode.hash) + valueType()
                        else -> throw IllegalStateException(addedNode.toString())
                    }

                    val valueMap =
                        mapOf("value" to hashChanged(from = nullValue, to = addedNode.hash) + valueChildren)

                    keyMap + valueMap
                }

                val removedDiff = removed.map { (removedKey, removedNode) ->
                    val keyMap = mapOf("key" to hashChanged(from = removedKey, to = nullValue))

                    val valueChildren = when (removedNode) {
                        is HashObject -> getDiff(removedNode, HashObject(nullValue, mapOf())) + objectType()
                        is HashArray -> getDiff(removedNode, HashArray(nullValue, listOf())) + arrayType()
                        is HashValue -> hashChanged(from = removedNode.hash, to = nullValue) + valueType()
                        else -> throw IllegalStateException(removedNode.toString())
                    }

                    val valueMap =
                        mapOf("value" to hashChanged(from = removedNode.hash, to = nullValue) + valueChildren)

                    keyMap + valueMap
                }

                val keyChangedDiff = keyChanged
                    .groupBy { it.second.hash }
                    .map { it.value }
                    .map {
                        assert(it.size == 2)

                        val from = when (first.hObject.containsKey(it.first().first)) {
                            true -> it[0].first
                            false -> it[1].first
                        }

                        val to = when (second.hObject.containsKey(it.first().first)) {
                            true -> it[0].first
                            false -> it[1].first
                        }

                        mapOf("key" to hashChanged(from, to))
                    }.also {
                        println("keyChangedDiff: $it")
                    }

                val valueChangedDiff = valueChanged
                    .groupBy { it.first }
                    .map { it.value }
                    .map {
                        assert(it.size == 2)
                        val key = it.first().first

                        val from = when (first.hObject.containsValue(it.first().second)) {
                            true -> it[0].second
                            false -> it[1].second
                        }

                        val to = when (second.hObject.containsValue(it.first().second)) {
                            true -> it[0].second
                            false -> it[1].second
                        }

                        val childDiffs = when (from) {
                            is HashObject -> getDiff(from, to)
                            is HashArray -> getDiff(from, to)
                            is HashValue -> valueToObjectOrArray(from, to)
                            else -> throw IllegalStateException()
                        }

                        val keyMap = mapOf("key" to hashSame(key))
                        val valueDiff = mapOf(
                            "value" to hashChanged(from = from.hash, to = to.hash) + childDiffs
                        )

                        keyMap + valueDiff
                    }.also {
                        println("valueChangedProgress: $it")
                    }

                val actualDiff = hashChanged(
                    from = first.hash,
                    to = second.hash
                ) + mapOf("children" to (addedDiff + removedDiff + keyChangedDiff + valueChangedDiff)) + objectType()

                println(ObjectMapper().writeValueAsString(actualDiff))
                return actualDiff
            }
            is HashArray -> {
                val removedObjectDiff = first.hObject.map { (removedKey, removedNode) ->
                    val keyMap = mapOf("key" to hashChanged(from = removedKey, to = nullValue))

                    val valueChildren = when (removedNode) {
                        is HashObject -> getDiff(removedNode, HashObject(nullValue, mapOf())) + objectType()
                        is HashArray -> getDiff(removedNode, HashArray(nullValue, listOf())) + arrayType()
                        is HashValue -> hashChanged(from = removedNode.hash, to = nullValue) + valueType()
                        else -> throw IllegalStateException(removedNode.toString())
                    }

                    val valueMap =
                        mapOf("value" to hashChanged(from = removedNode.hash, to = nullValue) + valueChildren)

                    keyMap + valueMap
                }

                val addedArrayDiff = second.hArray.map { addedChild ->
                    when (addedChild) {
                        is HashObject -> getDiff(HashObject(nullValue, mapOf()), addedChild)
                        is HashArray -> getDiff(HashArray(nullValue, listOf()), addedChild)
                        is HashValue -> hashChanged(from = nullValue, to = addedChild.hash) + valueType()
                        else -> throw IllegalStateException()
                    }
                }

                return hashChanged(
                    from = first.hash,
                    to = second.hash
                ) + mapOf("children" to removedObjectDiff + addedArrayDiff) + objectToArrayType()
            }
            is HashValue -> {
                val removedChildren =
                    getDiff(first, HashObject(nullValue, mapOf()))["children"] as List<Map<String, Any>>

                return hashChanged(
                    from = first.hash,
                    to = second.hash
                ) + mapOf("children" to removedChildren) + objectToValueType()
            }
            else -> throw IllegalStateException()
        }
    }

    private fun getDiff(first: HashArray, second: HashNode): Map<String, Any> {
        return when (second) {
            is HashObject -> {

                val removedArrayDiff = first.hArray.map { removedChild ->
                    when (removedChild) {
                        is HashObject -> getDiff(removedChild, HashObject(nullValue, mapOf()))
                        is HashArray -> getDiff(removedChild, HashArray(nullValue, listOf()))
                        is HashValue -> hashChanged(from = removedChild.hash, to = nullValue) + valueType()
                        else -> throw IllegalStateException()
                    }
                }

                val addedObjectDiff = second.hObject.map { (addedKey, addedNode) ->
                    val keyMap = mapOf("key" to hashChanged(from = nullValue, to = addedKey))
                    val valueChildren = when (addedNode) {
                        is HashObject -> getDiff(HashObject(nullValue, mapOf()), addedNode) + objectType()
                        is HashArray -> getDiff(HashArray(nullValue, listOf()), addedNode) + arrayType()
                        is HashValue -> hashChanged(from = nullValue, to = addedNode.hash) + valueType()
                        else -> throw IllegalStateException(addedNode.toString())
                    }

                    val valueMap =
                        mapOf("value" to hashChanged(from = nullValue, to = addedNode.hash) + valueChildren)

                    keyMap + valueMap
                }

                hashChanged(
                    from = first.hash,
                    to = second.hash
                ) + mapOf("children" to removedArrayDiff + addedObjectDiff) + arrayToObjectType()
            }
            is HashArray -> {
                val unChanged = first.hArray.intersect(second.hArray)
                val removed = first.hArray.subtract(unChanged)
                val added = second.hArray.subtract(unChanged)

                val addedDiff = added.map { addedChild ->
                    when (addedChild) {
                        is HashObject -> getDiff(HashObject(nullValue, mapOf()), addedChild)
                        is HashArray -> getDiff(HashArray(nullValue, listOf()), addedChild)
                        is HashValue -> hashChanged(from = nullValue, to = addedChild.hash) + valueType()
                        else -> throw IllegalStateException()
                    }
                }

                val removedDiff = removed.map { removedChild ->
                    when (removedChild) {
                        is HashObject -> getDiff(removedChild, HashObject(nullValue, mapOf()))
                        is HashArray -> getDiff(removedChild, HashArray(nullValue, listOf()))
                        is HashValue -> hashChanged(from = removedChild.hash, to = nullValue) + valueType()
                        else -> throw IllegalStateException()
                    }
                }

                hashChanged(
                    from = first.hash,
                    to = second.hash
                ) + mapOf("children" to addedDiff + removedDiff) + arrayType()
            }
            is HashValue -> {
                val removedChildren =
                    getDiff(first, HashArray(nullValue, listOf()))["children"] as List<Map<String, Any>>

                hashChanged(
                    from = first.hash,
                    to = second.hash
                ) + mapOf("children" to removedChildren) + arrayToValueType()
            }
            else -> throw IllegalStateException()
        }
    }

    private fun valueToObjectOrArray(first: HashValue, second: HashNode): Map<String, Any> {
        return when (second) {
            is HashObject -> getDiff(HashObject(first.hash, emptyMap()), second) + valueToObjectType()
            is HashArray -> getDiff(HashArray(first.hash, emptyList()), second) + valueToArrayType()
            is HashValue -> mapOf<String, Any>() + valueType()// no children
            else -> throw IllegalStateException()
        }
    }

    private fun hashChanged(from: String, to: String): Map<String, Any> = mapOf(
        "hash" to mapOf(
            "from" to from,
            "to" to to
        )
    )

    private fun hashSame(hash: String): Map<String, String> = mapOf("hash" to hash)

    private fun objectType(): Map<String, String> = mapOf("type" to "object")
    private fun objectToValueType(): Map<String, Any> = mapOf("type" to mapOf("from" to "object", "to" to "value"))
    private fun objectToArrayType(): Map<String, Any> = mapOf("type" to mapOf("from" to "object", "to" to "array"))
    private fun arrayType(): Map<String, String> = mapOf("type" to "array")
    private fun arrayToValueType(): Map<String, Any> = mapOf("type" to mapOf("from" to "array", "to" to "value"))
    private fun arrayToObjectType(): Map<String, Any> = mapOf("type" to mapOf("from" to "array", "to" to "object"))
    private fun valueType(): Map<String, String> = mapOf("type" to "value")
    private fun valueToObjectType(): Map<String, Any> = mapOf("type" to mapOf("from" to "value", "to" to "object"))
    private fun valueToArrayType(): Map<String, Any> = mapOf("type" to mapOf("from" to "value", "to" to "array"))
}
