package me.jameshunt.plinko.store.domain

import me.jameshunt.plinko.merkle.DiffParser
import java.time.OffsetDateTime

data class Commit(
    val documentId: Long,
    val createdAt: OffsetDateTime,
    val diff: DiffParser.ValueInfo
)
