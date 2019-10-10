package me.jameshunt.plinko

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.merkle.DiffGenerator
import me.jameshunt.plinko.merkle.JsonParser
import me.jameshunt.plinko.merkle.toHashObject
import me.jameshunt.plinko.store.Plinko
import me.jameshunt.plinko.store.domain.Commit
import org.junit.Assert
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun String.toJsonObject(): Map<String, Any?> = ObjectMapper()
    .readValue<Map<String, Any?>>(this, object : TypeReference<Map<String, Any?>>() {})

class DiffSenderTest {

    @Test
    fun testSender() {
        val before = "{}".toJsonObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testSend").document("test1")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(-1, OffsetDateTime.of(2019, 1,1,1,1,1,1, ZoneOffset.UTC), diff)

        val expected = """
            {
              "documentId": -1,
              "createdAt": "2019-01-01T01:01:01.000000001Z",
              "diff": {
                "hash": {
                  "from": "a8cfde6331bd59eb2ac96f8911c4b666",
                  "to": "4ec31704a3ec981be364071f6b6c9adc"
                },
                "children": [
                  {
                    "key": {
                      "hash": {
                        "from": "d41d8cd98f00b204e9800998ecf8427e",
                        "to": "9f9d51bc70ef21ca5c14f307980a29d8"
                      }
                    },
                    "value": {
                      "hash": {
                        "from": "d41d8cd98f00b204e9800998ecf8427e",
                        "to": "4101bef8794fed986e95dfb54850c68b"
                      },
                      "type": "value"
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "4101bef8794fed986e95dfb54850c68b": "nope",
                "9f9d51bc70ef21ca5c14f307980a29d8": "bob"
              }
            }
        """.trimIndent().toJsonObject()

        val objectMapper = ObjectMapper()
        
        Assert.assertEquals(
            objectMapper.writeValueAsString(expected),
            objectMapper.writeValueAsString(DiffSender.sendCommit(commit))
        )
    }
}
