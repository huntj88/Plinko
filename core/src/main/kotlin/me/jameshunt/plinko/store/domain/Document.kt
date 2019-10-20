package me.jameshunt.plinko.store.domain

import me.jameshunt.db.IndexedField
import me.jameshunt.plinko.merkle.*
import me.jameshunt.plinko.store.Plinko
import java.time.OffsetDateTime

typealias DocumentFromDB = me.jameshunt.db.Document
typealias DocumentIndex = me.jameshunt.db.SelectIndex

class Document(internal val data: DocumentFromDB) {

    //TODO: can be optimized so its not recomputed every time?
    private fun hashTree(asOfDate: OffsetDateTime): HashObject {
        val commits = Plinko.merkleDB.docCollection
            .getIncludedDocumentCommits(data.id)
            .filter { it.createdAt.toInstant() <= asOfDate.toInstant() }
            .map { DiffParser.parseDiff(it.diff) as DiffParser.ValueInfo.Object }

        return commits.fold(HashObject(nullValue, emptyMap())) { partialDocument, nextDiff ->
            DiffCommit.commit(partialDocument, nextDiff) as HashObject
        }
    }

    fun getData(asOfDate: OffsetDateTime = OffsetDateTime.now()): Map<String, Any?> {
        return JsonParser.write(hashTree(asOfDate).toJObject())
    }

    fun setData(json: Map<String, Any?>) {
        val dataToSet = JsonParser.read(json).also {
            // TODO: only extract the values in the diff
            it.extractJValues().forEach(Plinko.merkleDB.values::addJValue)
        }

        val diff = DiffGenerator.getDiff(hashTree(OffsetDateTime.now()), dataToSet.toHashObject())
        Plinko.merkleDB.docCollection.commitDiff(data.id, diff)
        dataToSet.setIndexData(getIndexedFields())
    }

    // TODO: only look at stuff in the diff to index instead
    private fun JObject.setIndexData(indexedFields: List<IndexedField>) {
        indexedFields.map { indexedField ->
            setIndexData(indexedField, indexedField.key)
        }
    }

    private fun JObject.setIndexData(indexedField: IndexedField, leftOver: String) {
        val firstKey = leftOver.substringBefore(".")
        val leftOverChild = leftOver.substringAfter(".")

        if (firstKey.isEmpty()) {
            println("index key: ${indexedField.key}, is not present in ${this@Document.data.id}/${this@Document.data.document_name}")
            return
        }

        when (val node = this.keyValues[JValue(firstKey)]) {
            is JObject -> node.setIndexData(indexedField, leftOverChild)
            is JValue -> Plinko.merkleDB.docCollection.addDocumentIndex(data.id, indexedField.key_hash, node.hash)
            null -> println("Index value not present: $firstKey")
            else -> {
                println("skipping indexing for: ")
                println(node)
            }
        }
    }

    fun collection(name: String): Collection {
        val col = Plinko.merkleDB.docCollection.getCollection(
            parentDocument = data.id,
            name = name
        )?.let { Collection(it) }

        return col ?: name.createIfCollectionDoesntExist()
    }

    private fun String.createIfCollectionDoesntExist(): Collection {
        println("collection with name: $this, does not exist")
        Plinko.merkleDB.docCollection.addCollection(data.id, this)
        println("added collection: $this")
        return collection(this)
    }

    internal fun getIndexedFields(): List<IndexedField> {
        return Plinko.merkleDB.docCollection.getIndexedFields(data.parent_collection_id)
        // TODO: index existing docs
    }
}
