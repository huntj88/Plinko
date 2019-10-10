package me.jameshunt.plinko.store.domain

import java.time.OffsetDateTime

data class Commit(
    val documentId: Long,
    val createdAt: OffsetDateTime,
    val diff: Map<String, Any>
)
