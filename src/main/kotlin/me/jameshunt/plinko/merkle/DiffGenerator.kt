package me.jameshunt.plinko.merkle

import com.fasterxml.jackson.databind.ObjectMapper

object DiffGenerator {

    fun getDiff(first: HashObject, second: HashNode): Map<String, Any> {
        if (first.hash == second.hash) TODO("no diff")

        when (second) {
            is HashObject -> {
                val unChanged = first.hObject.toList().filter { (keyA, nodeA) ->
                    val containsKey = second.hObject.containsKey(keyA)
                    val containsValue = second.hObject.toList().map { it.second.hash }.contains(nodeA.hash)
                    containsKey && containsValue
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
                    !keyChanged.contains(it) && !valueChanged.contains(it)
                }

                val removed = (changed.subtract(keyChanged + valueChanged)) - added

                println("added: $added")
                println("removed: $removed")

                val addedDiff = added.map { (addedKey, addedNode) ->
                    val keyMap = mapOf("key" to hashChanged(from = emptyOrNull, to = addedKey))
                    val valueChildren = when (addedNode) {
                        is HashObject -> getDiff(HashObject(emptyOrNull, mapOf()), addedNode) + objectType()
                        is HashArray -> TODO() //getDiff(HashArray(emptyOrNull, listOf()), addedNode)
                        is HashValue -> hashChanged(from = emptyOrNull, to = addedNode.hash) + valueType()
                        else -> throw IllegalStateException(addedNode.toString())
                    }

                    val valueMap =
                        mapOf("value" to hashChanged(from = emptyOrNull, to = addedNode.hash) + valueChildren)

                    keyMap + valueMap
                }

                val removedDiff = removed.map { (removedKey, removedNode) ->
                    val keyMap = mapOf("key" to hashChanged(from = removedKey, to = emptyOrNull))

                    val valueChildren = when (removedNode) {
                        is HashObject -> getDiff(removedNode, HashObject(emptyOrNull, mapOf())) + objectType()
                        is HashArray -> TODO() //getDiff(HashArray(emptyOrNull, listOf()), addedNode)
                        is HashValue -> hashChanged(from = removedNode.hash, to = emptyOrNull) + valueType()
                        else -> throw IllegalStateException(removedNode.toString())
                    }

                    val valueMap =
                        mapOf("value" to hashChanged(from = removedNode.hash, to = emptyOrNull) + valueChildren)

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

                        val childDiffs = when(from) {
                            is HashObject -> getDiff(from, to)
                            is HashArray -> TODO()
                            is HashValue -> getDiff(from, to)
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
            is HashArray -> TODO()
            is HashValue -> {
                val removedChildren = getDiff(first, HashObject(emptyOrNull, mapOf()))["children"] as List<Map<String, Any>>

                return hashChanged(
                    from = first.hash,
                    to = second.hash
                ) + mapOf("children" to removedChildren) + valueType()
            }
            else -> throw IllegalStateException()
        }
    }

    // basically handles changing from simpleValue to object/array
    private fun getDiff(first: HashValue, second: HashNode): Map<String, Any> {
        return when(second) {
            is HashObject -> getDiff(HashObject(first.hash, emptyMap()), second) + objectType()
            is HashArray -> TODO()
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
    private fun arrayType(): Map<String, String> = mapOf("type" to "array")
    private fun valueType(): Map<String, String> = mapOf("type" to "value")
}
