package me.jameshunt.plinko.merkle

object DiffCommit {

    fun commit(hashObject: HashObject, diff: DiffParser.ValueInfo): HashNode {
        when (diff) {
            is DiffParser.ValueInfo.Object -> {
                //if no diff then just return the original hashObject
                if (diff.to == hashObject.hash) return hashObject
                if (diff.from != hashObject.hash) throw IllegalStateException("cannot apply commit")

                val keysToCheckIfChildChanged = diff.children
                    .map { it.key }
                    .map { diffKeyInfo ->
                        when (diffKeyInfo) {
                            is DiffParser.KeyInfo.KeySame -> diffKeyInfo.hash
                            is DiffParser.KeyInfo.KeyChanged -> diffKeyInfo.from
                        }
                    }
                val childrenNotChanged = hashObject.hObject.mapNotNull { (childKey, node) ->
                    val changed = keysToCheckIfChildChanged.contains(childKey)
                    when {
                        changed -> null
                        else -> childKey to node
                    }
                }.toMap()

                val changed = diff.children
                    .map { (keyInfo, valueInfo) ->
                        when (keyInfo) {
                            is DiffParser.KeyInfo.KeySame -> {
                                assert(valueInfo != null)
                                val childTree = hashObject.hObject[keyInfo.hash]!!
                                when (childTree) {
                                    is HashObject -> keyInfo.hash to commit(childTree, valueInfo!!)
                                    is HashArray -> keyInfo.hash to commit(childTree, valueInfo!!)
                                    is HashValue -> keyInfo.hash to valueInfo!!.toHashVersion()
                                    else -> throw IllegalStateException()
                                }
                            }
                            is DiffParser.KeyInfo.KeyChanged -> {
                                // if value info not null, new key/value pair added
                                // otherwise, only key changed, and copy value from previous object
                                when (valueInfo) {
                                    is DiffParser.ValueInfo.Object,
                                    is DiffParser.ValueInfo.Array,
                                    is DiffParser.ValueInfo.Value -> keyInfo.to to valueInfo.toHashVersion()
                                    null -> keyInfo.to to hashObject.hObject[keyInfo.from]!!
                                    else -> throw IllegalStateException()
                                }
                            }
                        }
                    }
                    .filter { (key, _) -> key != nullValue }
                    .toMap()

                return HashObject(diff.to, changed + childrenNotChanged)
            }
            is DiffParser.ValueInfo.ObjectToArray,
            is DiffParser.ValueInfo.ObjectToValue -> return diff.toHashVersion()

            is DiffParser.ValueInfo.Array,
            is DiffParser.ValueInfo.Value,
            is DiffParser.ValueInfo.ArrayToObject,
            is DiffParser.ValueInfo.ArrayToValue,
            is DiffParser.ValueInfo.ValueToObject,
            is DiffParser.ValueInfo.ValueToArray -> throw IllegalStateException()
        }
    }

    private fun commit(hashArray: HashArray, diff: DiffParser.ValueInfo): HashNode {
        when (diff) {
            is DiffParser.ValueInfo.Array -> {
                val hashesToCheckIfChildChanged = diff.children.map {
                    when (it) {
                        is DiffParser.ValueInfo.Value -> it.from
                        is DiffParser.ValueInfo.Object -> it.from
                        is DiffParser.ValueInfo.Array -> it.from
                        else -> throw IllegalStateException()
                    }
                }
                val childrenNotChanged = hashArray.hArray.mapNotNull { node ->
                    val changed = hashesToCheckIfChildChanged.contains(node.hash)
                    when {
                        changed -> null
                        else -> node
                    }
                }

                val childrenChanged = diff.children
                    .map { valueInfo -> valueInfo.toHashVersion() }
                    .filter { it.hash != nullValue }

                return HashArray(diff.to, childrenNotChanged + childrenChanged)
            }
            is DiffParser.ValueInfo.ArrayToObject,
            is DiffParser.ValueInfo.ArrayToValue -> return diff.toHashVersion()

            is DiffParser.ValueInfo.Object,
            is DiffParser.ValueInfo.Value,
            is DiffParser.ValueInfo.ObjectToArray,
            is DiffParser.ValueInfo.ObjectToValue,
            is DiffParser.ValueInfo.ValueToObject,
            is DiffParser.ValueInfo.ValueToArray -> throw IllegalStateException()
        }
    }

    private fun DiffParser.ValueInfo.toHashVersion(): HashNode {
        return when (this) {
            is DiffParser.ValueInfo.Value -> this.toHashValue()
            is DiffParser.ValueInfo.Object -> this.toHashObject()
            is DiffParser.ValueInfo.Array -> this.toHashArray()
            is DiffParser.ValueInfo.ObjectToArray -> this.toHashArray()
            is DiffParser.ValueInfo.ObjectToValue -> this.toHashValue()
            is DiffParser.ValueInfo.ArrayToObject -> this.toHashObject()
            is DiffParser.ValueInfo.ArrayToValue -> this.toHashValue()
            is DiffParser.ValueInfo.ValueToObject -> this.toHashObject()
            is DiffParser.ValueInfo.ValueToArray -> this.toHashArray()
        }
    }

    private fun DiffParser.ValueInfo.Value.toHashValue() = HashValue(this.to)

    private fun DiffParser.ValueInfo.Array.toHashArray(): HashArray {
        val hArray = this.children.map { it.toHashVersion() }.filter { it.hash != nullValue }
        return HashArray(this.to, hArray)
    }

    private fun DiffParser.ValueInfo.Object.toHashObject(): HashObject {
        val children = this.children
            .map { (keyInfo, value) ->
                val key = when (keyInfo) {
                    is DiffParser.KeyInfo.KeySame -> keyInfo.hash
                    is DiffParser.KeyInfo.KeyChanged -> keyInfo.to
                }
                key to value!!.toHashVersion()
            }
            .toMap()

        return HashObject(this.to, children)
    }

    private fun DiffParser.ValueInfo.ObjectToValue.toHashValue() = HashValue(this.to)
    private fun DiffParser.ValueInfo.ArrayToValue.toHashValue() = HashValue(this.to)
    private fun DiffParser.ValueInfo.ValueToObject.toHashObject(): HashObject {
        return DiffParser.ValueInfo.Object(from, to, objectChildren).toHashObject()
    }

    private fun DiffParser.ValueInfo.ValueToArray.toHashArray(): HashArray {
        return DiffParser.ValueInfo.Array(from, to, arrayChildren).toHashArray()
    }

    private fun DiffParser.ValueInfo.ObjectToArray.toHashArray(): HashArray {
        return DiffParser.ValueInfo.Array(from, to, arrayChildren).toHashArray()
    }

    private fun DiffParser.ValueInfo.ArrayToObject.toHashObject(): HashObject {
        return DiffParser.ValueInfo.Object(from, to, objectChildren).toHashObject()
    }
}
