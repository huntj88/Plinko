package me.jameshunt.plinko.store

import me.jameshunt.plinko.store.db.MerkleDB
import me.jameshunt.plinko.store.domain.Collection

object Plinko {
    fun collection(name: String): Collection {
        return MerkleDB.docCollection.run {
            val col = this.getCollection(
                parentDocument = 1,
                name = name
            )?.let { Collection(it) }

            col ?: let {
                println("collection with name: $name, does not exist")
                addCollection(1,name)
                println("added collection: $name")
                collection(name)
            }
        }
    }
}
