package me.jameshunt.plinko.store.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.db.*
import me.jameshunt.db.Collection
import me.jameshunt.plinko.merkle.DiffParser
import me.jameshunt.plinko.store.domain.Commit
import me.jameshunt.plinko.store.domain.DocumentIndex
import me.jameshunt.plinko.store.domain.format
import me.jameshunt.plinko.store.domain.now
import org.apache.commons.codec.digest.DigestUtils
import java.time.OffsetDateTime

typealias DocumentId = Long
typealias CollectionId = Long

class DocumentAndCollectionDB(private val queries: DocumentsAndCollectionsQueries) {

    fun addCollection(parentDocument: DocumentId, name: String) {
        queries.addCollection(
            parent_document_id = parentDocument,
            collection_name = name,
            created_at = now
        )
    }

    fun getCollection(parentDocument: DocumentId, name: String): Collection? {
        return queries.selectCollection(
            parent_document_id = parentDocument,
            collection_name = name
        ).executeAsOneOrNull()
    }

    fun setIndex(collectionId: CollectionId, key: String) {
        val keyHash = DigestUtils.md5Hex(key)
        queries.setIndex(collectionId, keyHash, now, key)
    }

    fun getIndexedFields(collectionId: CollectionId): List<IndexedField> {
        return queries.indexedField(collectionId).executeAsList()
    }

    fun addDocument(parentCollection: CollectionId, name: String) {
        queries.addDocument(
            parent_collection_id = parentCollection,
            document_name = name,
            created_at = now
        )
    }

    fun getDocument(parentCollection: CollectionId, name: String): Document? {
        return queries.selectDocument(
            parent_collection_id = parentCollection,
            document_name = name
        ).executeAsOneOrNull()
    }

    fun getDocuments(parentCollection: CollectionId, ids: Set<DocumentId>): List<Document> {
        return queries.selectDocuments(
            parentCollection,
            ids
        ).executeAsList()
    }

    private val objectMapper = ObjectMapper()

    fun getDocumentCommits(documentId: DocumentId): List<Commit> {
        return queries.selectCommits(documentId).executeAsList().map {
            Commit(
                documentId = it.document_id,
                createdAt = OffsetDateTime.parse(it.created_at),
                diff = DiffParser.parseDiff(it.diff.toDiffJson())
            )
        }
    }

    fun commitDiff(documentId: DocumentId, diff: Map<String, Any>) {
        queries.commitDiff(
            document_id = documentId,
            created_at = now,
            diff = objectMapper.writeValueAsString(diff)
        )
    }

    fun addDocumentIndex(documentId: DocumentId, keyHash: String, valueHash: String) {
        // TODO: check to make sure the most recent index for the same key has a different value hash
        // TODO: may not be necessary if i only re-index values in the diff
        queries.addDocumentIndex(documentId, keyHash, valueHash, now)
    }

    fun getDocumentIndex(parentCollectionId: CollectionId, keyHash: String, queryDate: OffsetDateTime): List<DocumentIndex> {
        return queries.selectIndex(parentCollectionId, keyHash, queryDate.format()).executeAsList()
    }

    private fun String.toDiffJson(): Map<String, Any> {
        val type = object : TypeReference<Map<String, Any>>() {}
        return objectMapper.readValue(this, type)
    }
}
