package me.jameshunt.plinko.merkle

object DiffCommit {

    fun commit(hashObject: HashObject, diff: DiffParser.ValueInfo.Object): HashNode {
        // if no diff then just return the original hashObject
        if (diff.to == hashObject.hash) return hashObject

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
                            is DiffParser.ValueInfo.Value -> keyInfo.to to valueInfo.toHashVersion()
                            null -> keyInfo.to to hashObject.hObject[keyInfo.from]!!
                            else -> throw IllegalStateException()
                        }
                    }
                }
            }
            .filter { (key, _) -> key != nullValue }
            .toMap()

        return HashObject(diff.to, changed).also { println(it) }
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
