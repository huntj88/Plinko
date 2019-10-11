package me.jameshunt.plinko.transport

import me.jameshunt.plinko.store.domain.Commit
import java.time.OffsetDateTime

object DiffTransportReceiver {

    data class CommitAndValues(
        val commit: Commit,
        val newValues: Map<String, Any?>
    )

    fun asCommitAndValues(json: Map<String, Any?>): CommitAndValues {
        val commit = Commit(
            json["documentId"] as Long,
            (json["createdAt"] as String).let(OffsetDateTime::parse),
            (json["diff"] as Map<String, Any>)
        )

        val newValues = json["values"] as Map<String, Any?>
        return CommitAndValues(commit, newValues)
    }
}
