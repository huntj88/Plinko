package me.jameshunt.plinko.store.domain

import me.jameshunt.plinko.merkle.JValue
import me.jameshunt.plinko.merkle.ValueType
import me.jameshunt.plinko.store.db.CollectionId
import me.jameshunt.plinko.store.db.DocumentId
import me.jameshunt.plinko.store.db.MerkleDB
import org.apache.commons.codec.digest.DigestUtils

typealias CollectionFromDB = me.jameshunt.db.Collection

class Collection(internal val data: CollectionFromDB) {
    fun document(name: String): Document {
        val doc = MerkleDB.docCollection.getDocument(
            parentCollection = data.id,
            name = name
        )?.let { Document(it) }

        return doc ?: name.createIfDocumentDoesntExist()
    }

    fun setIndex(key: String) {
        assert(key.split(".").isNotEmpty())
        assert(key.matches("[a-zA-Z0-9.]+".toRegex()))
        MerkleDB.docCollection.setIndex(data.id, key)

        // TODO: index existing docs
    }

    fun query(queryBuilder: QueryBuilder.() -> QueryBuilder.QueryPart): List<Document> {
        return QueryBuilder(data.id)
            .run(queryBuilder)
            .found
            .let { docIds ->
                MerkleDB.docCollection
                    .getDocuments(data.id, docIds)
                    .map(::Document)
            }
    }

    private fun String.createIfDocumentDoesntExist(): Document {
        println("document with name: $this, does not exist")
        MerkleDB.docCollection.addDocument(data.id, this)
        println("added document: $this")
        return document(this)
    }
}

class QueryBuilder(private val collectionId: CollectionId) {

    fun whereEqualTo(key: String, value: Any?): QueryPart {
        return MerkleDB.docCollection
            .getDocumentIndex(collectionId, DigestUtils.md5Hex(key))
            .filter { it.value_hash == JValue(value).hash }
            .map { it.document_id }
            .let { QueryPart(it.toSet()) }
    }

    fun whereGreaterThan(key: String, value: Any?): QueryPart {
        val jQueryValue = JValue(value)

        return MerkleDB.docCollection
            .getDocumentIndex(collectionId, DigestUtils.md5Hex(key))
            .let { indexes ->
                val jValues = MerkleDB.values.getJValues(indexes.map { it.value_hash })
                indexes.map { index -> index to jValues.first { it.hash == index.value_hash } }
            }
            .filter { (_, jValue) ->
                jValue.valueType == jQueryValue.valueType || (jValue.value is Number && jQueryValue.value is Number)
            }
            .filter { (_, jValue) ->
                when (jValue.valueType) {
                    ValueType.String -> jValue.value as String > jQueryValue.value as String
                    ValueType.Int, ValueType.Double -> (jValue.value as Number).toDouble() > (jQueryValue.value as Number).toDouble()
                    ValueType.Boolean -> jValue.value as Boolean > jQueryValue.value as Boolean
                    ValueType.Null -> TODO()
                }
            }
            .map { (index, _) -> index.document_id }
            .let { QueryPart(it.toSet()) }
    }

    fun whereLessThan(key: String, value: Any?): QueryPart {
        val jQueryValue = JValue(value)

        return MerkleDB.docCollection
            .getDocumentIndex(collectionId, DigestUtils.md5Hex(key))
            .let { indexes ->
                val jValues = MerkleDB.values.getJValues(indexes.map { it.value_hash })
                indexes.map { index -> index to jValues.first { it.hash == index.value_hash } }
            }
            .filter { (_, jValue) ->
                jValue.valueType == jQueryValue.valueType || (jValue.value is Number && jQueryValue.value is Number)
            }
            .filter { (_, jValue) ->
                when (jValue.valueType) {
                    ValueType.String -> (jValue.value as String) < jQueryValue.value as String
                    ValueType.Int, ValueType.Double -> (jValue.value as Number).toDouble() < (jQueryValue.value as Number).toDouble()
                    ValueType.Boolean -> (jValue.value as Boolean) < jQueryValue.value as Boolean
                    ValueType.Null -> TODO()
                }
            }
            .map { (index, _) -> index.document_id }
            .let { QueryPart(it.toSet()) }
    }

    data class QueryPart(internal val found: Set<DocumentId>)

    infix fun QueryPart.and(queryPart: QueryPart): QueryPart {
        return QueryPart(this.found.intersect(queryPart.found))
    }

    infix fun QueryPart.or(queryPart: QueryPart): QueryPart {
        return QueryPart(this.found.toSet() + queryPart.found.toSet())
    }
}
