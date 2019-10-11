package me.jameshunt.plinko.client

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import me.jameshunt.plinko.transport.DiffTransportFormatter
import me.jameshunt.plinko.store.domain.Commit

object PlinkoClient {
    private val objectMapper = ObjectMapper()

    suspend fun sendCommit(commit: Commit) {

        val json = DiffTransportFormatter.format(commit).let(objectMapper::writeValueAsString)

        val client = HttpClient()

        client.post<ByteArray>(urlString = "https://127.0.0.1:8080") {
            body = json
        }

        client.close()
    }

}
