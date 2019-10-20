package me.jameshunt.plinko.store.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.db.Collection
import me.jameshunt.db.Document
import me.jameshunt.db.DocumentsAndCollectionsQueries
import me.jameshunt.db.IndexedField
import me.jameshunt.plinko.store.domain.Commit
import me.jameshunt.plinko.store.domain.DocumentIndex
import me.jameshunt.plinko.store.domain.format
import me.jameshunt.plinko.store.domain.now
import org.apache.commons.codec.digest.DigestUtils
import java.time.OffsetDateTime

typealias DocumentId = Long
typealias CollectionId = Long

class DocumentAndCollectionDB(
    private val queries: DocumentsAndCollectionsQueries,
    private val objectMapper: ObjectMapper
) {

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
            created_at = now,
            included_commit_hashes = ""
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

    fun getDocumentCommits(documentId: DocumentId): List<Commit> {
        return queries.selectCommits(documentId).executeAsList().map {
            Commit(
                documentId = it.document_id,
                createdAt = OffsetDateTime.parse(it.created_at),
                diff = it.diff.toDiffJson()
            )
        }
    }

    fun commitDiff(documentId: DocumentId, diff: Map<String, Any>) {
        val newCommitHash = diff["hash"]
            .let { it as Map<String, String> }
            .let { it["to"]!! }

        queries.transaction {
            val existingHashes = queries
                .selectDocumentById(documentId)
                .executeAsOne()
                .included_commit_hashes

            val newIncludedHashes = when(existingHashes.isEmpty()) {
                true -> newCommitHash
                false -> "$existingHashes,$newCommitHash"
            }

            queries.updateIncludedCommits(newIncludedHashes, documentId)
            queries.commitDiff(
                document_id = documentId,
                hash = newCommitHash,
                created_at = now,
                diff = objectMapper.writeValueAsString(diff)
            )
        }
    }

    fun addDocumentIndex(documentId: DocumentId, keyHash: String, valueHash: String) {
        // TODO: check to make sure the most recent index for the same key has a different value hash
        // TODO: may not be necessary if i only re-index values in the diff
        queries.addDocumentIndex(documentId, keyHash, valueHash, now)
    }

    fun getDocumentIndex(
        parentCollectionId: CollectionId,
        keyHash: String,
        queryDate: OffsetDateTime
    ): List<DocumentIndex> {
        return queries.selectIndex(parentCollectionId, keyHash, queryDate.format()).executeAsList()
    }

    private fun String.toDiffJson(): Map<String, Any> {
        val type = object : TypeReference<Map<String, Any>>() {}
        return objectMapper.readValue(this, type)
    }
}
