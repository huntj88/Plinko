package me.jameshunt.plinko.merkle

object DiffRevert {

    // same as DiffCommit but with the from/to hashes swapped, and change type methods inversed

    fun revert(hashObject: HashObject, diff: DiffParser.ValueInfo): HashNode {
        when (diff) {
            is DiffParser.ValueInfo.Object -> {
                //if no diff then just return the original hashObject
                if (diff.from == hashObject.hash) return hashObject

                val keysToCheckIfChildChanged = diff.children
                    .map { it.key }
                    .map { diffKeyInfo ->
                        when (diffKeyInfo) {
                            is DiffParser.KeyInfo.KeySame -> diffKeyInfo.hash
                            is DiffParser.KeyInfo.KeyChanged -> diffKeyInfo.to
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
                                    is HashObject -> keyInfo.hash to revert(childTree, valueInfo!!)
                                    is HashArray -> keyInfo.hash to revert(childTree, valueInfo!!)
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
                                    is DiffParser.ValueInfo.Value -> keyInfo.from to valueInfo.toHashVersion()
                                    null -> keyInfo.from to hashObject.hObject[keyInfo.to]!!
                                    else -> throw IllegalStateException()
                                }
                            }
                        }
                    }
                    .filter { (key, _) -> key != nullValue }
                    .toMap()

                return HashObject(diff.from, changed + childrenNotChanged)
            }
            // these seem backward, but revert instead of commit, compare to committer
            is DiffParser.ValueInfo.ArrayToObject,
            is DiffParser.ValueInfo.ValueToObject -> return diff.toHashVersion()

            is DiffParser.ValueInfo.Array,
            is DiffParser.ValueInfo.Value,
            is DiffParser.ValueInfo.ObjectToArray,
            is DiffParser.ValueInfo.ArrayToValue,
            is DiffParser.ValueInfo.ObjectToValue,
            is DiffParser.ValueInfo.ValueToArray -> throw IllegalStateException()
        }
    }

    private fun revert(hashArray: HashArray, diff: DiffParser.ValueInfo): HashNode {
        when (diff) {
            is DiffParser.ValueInfo.Array -> {
                val hashesToCheckIfChildChanged = diff.children.map {
                    when (it) {
                        is DiffParser.ValueInfo.Value -> it.to
                        is DiffParser.ValueInfo.Object -> it.to
                        is DiffParser.ValueInfo.Array -> it.to
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

                return HashArray(diff.from, childrenNotChanged + childrenChanged)
            }
            // these seem backward, but revert instead of commit, compare to committer
            is DiffParser.ValueInfo.ObjectToArray,
            is DiffParser.ValueInfo.ValueToArray -> return diff.toHashVersion()

            is DiffParser.ValueInfo.Object,
            is DiffParser.ValueInfo.Value,
            is DiffParser.ValueInfo.ArrayToObject,
            is DiffParser.ValueInfo.ObjectToValue,
            is DiffParser.ValueInfo.ValueToObject,
            is DiffParser.ValueInfo.ArrayToValue -> throw IllegalStateException()
        }
    }

    private fun DiffParser.ValueInfo.toHashVersion(): HashNode {
        return when (this) {
            is DiffParser.ValueInfo.Value -> this.toHashValue()
            is DiffParser.ValueInfo.Object -> this.toHashObject()
            is DiffParser.ValueInfo.Array -> this.toHashArray()
            is DiffParser.ValueInfo.ObjectToArray -> this.toHashObject()
            is DiffParser.ValueInfo.ObjectToValue -> this.toHashObject()
            is DiffParser.ValueInfo.ArrayToObject -> this.toHashArray()
            is DiffParser.ValueInfo.ArrayToValue -> this.toHashArray()
            is DiffParser.ValueInfo.ValueToObject -> this.toHashValue()
            is DiffParser.ValueInfo.ValueToArray -> this.toHashValue()
        }
    }

    private fun DiffParser.ValueInfo.Value.toHashValue() = HashValue(this.from)

    private fun DiffParser.ValueInfo.Array.toHashArray(): HashArray {
        val hArray = this.children.map { it.toHashVersion() }.filter { it.hash != nullValue }
        return HashArray(this.from, hArray)
    }

    private fun DiffParser.ValueInfo.Object.toHashObject(): HashObject {
        val children = this.children
            .map { (keyInfo, value) ->
                val key = when (keyInfo) {
                    is DiffParser.KeyInfo.KeySame -> keyInfo.hash
                    is DiffParser.KeyInfo.KeyChanged -> keyInfo.from
                }
                key to value!!.toHashVersion()
            }
            .toMap()

        return HashObject(this.from, children)
    }

    // All of these method names for converting to HashNode have the name swapped since its a revert
    // Compare to DiffCommit

    private fun DiffParser.ValueInfo.ValueToObject.toHashValue() = HashValue(this.from)
    private fun DiffParser.ValueInfo.ValueToArray.toHashValue() = HashValue(this.from)

    // from and to's and not switched here because they delegate to something that is switch
    private fun DiffParser.ValueInfo.ObjectToValue.toHashObject(): HashObject {
        return DiffParser.ValueInfo.Object(from, to, objectChildren).toHashObject()
    }

    private fun DiffParser.ValueInfo.ArrayToValue.toHashArray(): HashArray {
        return DiffParser.ValueInfo.Array(from, to, arrayChildren).toHashArray()
    }

    private fun DiffParser.ValueInfo.ArrayToObject.toHashArray(): HashArray {
        return DiffParser.ValueInfo.Array(from, to, arrayChildren).toHashArray()
    }

    private fun DiffParser.ValueInfo.ObjectToArray.toHashObject(): HashObject {
        return DiffParser.ValueInfo.Object(from, to, objectChildren).toHashObject()
    }
}
