package me.jameshunt.plinko.merkle

object DiffRevert {

    // same as DiffCommit but with the from/to hashes swapped, and change type methods inversed

    fun revert(hashObject: HashObject, diff: DiffParser.ValueInfo.Object): HashNode {
        // if no diff then just return the original hashObject
        if (diff.from == hashObject.hash) return hashObject

        val changed = diff.children
            .map { (keyInfo, valueInfo) ->
                when (keyInfo) {
                    is DiffParser.KeyInfo.KeySame -> {
                        assert(valueInfo != null)
                        keyInfo.hash to valueInfo!!.toHashVersion()
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

        return HashObject(diff.from, changed).also { println(it) }
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
