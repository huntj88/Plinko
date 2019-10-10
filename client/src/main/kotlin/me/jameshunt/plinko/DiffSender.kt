package me.jameshunt.plinko

import me.jameshunt.plinko.merkle.DiffParser
import me.jameshunt.plinko.store.db.MerkleDB
import me.jameshunt.plinko.store.domain.Commit
import me.jameshunt.plinko.store.domain.format

object DiffSender {

    fun sendCommit(commit: Commit): Map<String, Any?> {
        return mapOf(
            "documentId" to commit.documentId,
            "createdAt" to commit.createdAt.format(),
            "diff" to commit.diff,
            "values" to DiffParser.parseDiff(commit.diff)
                .getValueHashes()
                .let(MerkleDB.values::getJValues)
                .map { it.hash to it.value }
                .toMap()
        )
    }

    private fun DiffParser.ValueInfo.getValueHashes(): List<String> {
        return when (this) {
            is DiffParser.ValueInfo.Object -> getValueHashes()
            is DiffParser.ValueInfo.Array -> getValueHashes()
            is DiffParser.ValueInfo.Value -> hashValue()
            is DiffParser.ValueInfo.ObjectToArray -> getValueHashes()
            is DiffParser.ValueInfo.ObjectToValue -> hashValue()
            is DiffParser.ValueInfo.ArrayToObject -> getValueHashes()
            is DiffParser.ValueInfo.ArrayToValue -> hashValue()
            is DiffParser.ValueInfo.ValueToObject -> getValueHashes()
            is DiffParser.ValueInfo.ValueToArray -> getValueHashes()
        }
    }

    private fun DiffParser.ValueInfo.Object.getValueHashes(): List<String> {
        return this.children.children()
    }

    private fun DiffParser.ValueInfo.ArrayToObject.getValueHashes(): List<String> {
        return this.objectChildren.children() + arrayChildren.children()
    }

    private fun DiffParser.ValueInfo.ValueToObject.getValueHashes(): List<String> {
        return this.objectChildren.children() + from
    }

    private fun Map<DiffParser.KeyInfo, DiffParser.ValueInfo?>.children(): List<String> {
        return this.flatMap { (key, value) ->
            key.hashValue() + (value?.hashValue() ?: emptyList())
        }
    }

    private fun DiffParser.ValueInfo.Array.getValueHashes(): List<String> {
        return this.children.children()
    }

    private fun DiffParser.ValueInfo.ObjectToArray.getValueHashes(): List<String> {
        return this.arrayChildren.children() + objectChildren.children()
    }

    private fun DiffParser.ValueInfo.ValueToArray.getValueHashes(): List<String> {
        return this.arrayChildren.children() + from
    }

    private fun List<DiffParser.ValueInfo>.children(): List<String> {
        return this.flatMap { value -> value.hashValue() }
    }

    private fun DiffParser.KeyInfo.hashValue(): List<String> {
        return when(this) {
            is DiffParser.KeyInfo.KeySame -> listOf(hash)
            is DiffParser.KeyInfo.KeyChanged -> listOf(from, to)
        }
    }

    private fun DiffParser.ValueInfo.hashValue(): List<String> {
        return when (this) {
            is DiffParser.ValueInfo.Value -> listOf(from, to)
            is DiffParser.ValueInfo.ArrayToValue -> listOf(from, to)
            is DiffParser.ValueInfo.ObjectToValue -> listOf(from, to)
            is DiffParser.ValueInfo.Object,
            is DiffParser.ValueInfo.Array,
            is DiffParser.ValueInfo.ObjectToArray,
            is DiffParser.ValueInfo.ArrayToObject,
            is DiffParser.ValueInfo.ValueToObject,
            is DiffParser.ValueInfo.ValueToArray -> emptyList()
        }
    }
}
