package me.jameshunt.plinko.store

import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.merkle.JValue
import me.jameshunt.plinko.store.db.MerkleDB
import me.jameshunt.plinko.store.domain.Collection
import me.jameshunt.plinko.store.domain.Document
import me.jameshunt.plinko.store.domain.DocumentFromDB

object Plinko {

    internal val objectMapper = ObjectMapper()
    internal val merkleDB = MerkleDB()

    // root doc to bootstrap access
    private val rootDoc = Document(object : DocumentFromDB {
        init {
            // TODO: only if first run
            merkleDB.docCollection.addDocument(
                parentCollection = 0,
                name = "rootDocument"
            )
        }

        override val id: Long = 1
        override val parent_collection_id: Long
            get() = throw IllegalStateException()
        override val document_name: String
            get() = throw IllegalStateException()
        override val created_at: String
            get() = throw IllegalStateException()
        override val included_commit_hashes: String
            get() = throw IllegalStateException()
    })

    fun collection(name: String): Collection {
        return rootDoc.collection(name)
    }
}
