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

class DiffTransportFormatterTest {
    private val objectMapper = ObjectMapper()
    private val commitTimeCreatedAt = OffsetDateTime.of(
        2019,
        1,
        1,
        1,
        1,
        1,
        1000000,
        ZoneOffset.UTC
    )

    fun Commit.asJson(): String = objectMapper.writeValueAsString(DiffTransportFormatter.format(this))

    @Test
    fun `add key-simpleValue to object`() {
        val before = "{}".toJsonObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("add key-simpleValue to object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff)

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
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
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `remove key-simpleValue from object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonObject()
        val after = "{}".toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("remove key-simpleValue from object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff)

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "4ec31704a3ec981be364071f6b6c9adc",
                  "to": "a8cfde6331bd59eb2ac96f8911c4b666"
                },
                "children": [
                  {
                    "key": {
                      "hash": {
                        "from": "9f9d51bc70ef21ca5c14f307980a29d8",
                        "to": "d41d8cd98f00b204e9800998ecf8427e"
                      }
                    },
                    "value": {
                      "hash": {
                        "from": "4101bef8794fed986e95dfb54850c68b",
                        "to": "d41d8cd98f00b204e9800998ecf8427e"
                      },
                      "type": "value"
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change key in object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        val after = """
            {
              "susan": "nope"
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko
            .collection("testFormat")
            .document("change key in object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "4ec31704a3ec981be364071f6b6c9adc",
                  "to": "e34d8e4dfc3deb42f0162cf9d5d26f13"
                },
                "children": [
                  {
                    "key": {
                      "hash": {
                        "from": "9f9d51bc70ef21ca5c14f307980a29d8",
                        "to": "ac575e3eecf0fa410518c2d3a2e7209f"
                      }
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "ac575e3eecf0fa410518c2d3a2e7209f": "susan"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change simple value to different simple value`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        val after = """
            {
              "bob": "yep"
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko
            .collection("testFormat")
            .document("change simple value to different simple value")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "4ec31704a3ec981be364071f6b6c9adc",
                  "to": "c7f44b81161af068175584387a7b4ba8"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "4101bef8794fed986e95dfb54850c68b",
                        "to": "9348ae7851cf3ba798d9564ef308ec25"
                      },
                      "type": "value"
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "9348ae7851cf3ba798d9564ef308ec25": "yep"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change simple value to object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        val after = """
            {
              "bob": {
                "wow": true
              }
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko
            .collection("testFormat")
            .document("change simple value to object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "4ec31704a3ec981be364071f6b6c9adc",
                  "to": "2d3ced6443d9a19d2b87bb06656283f6"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "4101bef8794fed986e95dfb54850c68b",
                        "to": "61c6593e5704bf45570b6fe88c27f423"
                      },
                      "type": {
                        "from": "value",
                        "to": "object"
                      },
                      "children": [
                        {
                          "key": {
                            "hash": {
                              "from": "d41d8cd98f00b204e9800998ecf8427e",
                              "to": "bcedc450f8481e89b1445069acdc3dd9"
                            }
                          },
                          "value": {
                            "hash": {
                              "from": "d41d8cd98f00b204e9800998ecf8427e",
                              "to": "b326b5062b2f0e69046810717534cb09"
                            },
                            "type": "value"
                          }
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "b326b5062b2f0e69046810717534cb09": true,
                "bcedc450f8481e89b1445069acdc3dd9": "wow"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change object to simple value`() {
        val before = """
            {
              "bob": {
                "wow": true
              }
            }
        """.toJsonObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("change object to simple value")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "2d3ced6443d9a19d2b87bb06656283f6",
                  "to": "4ec31704a3ec981be364071f6b6c9adc"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "61c6593e5704bf45570b6fe88c27f423",
                        "to": "4101bef8794fed986e95dfb54850c68b"
                      },
                      "type": {
                        "from": "object",
                        "to": "value"
                      },
                      "children": [
                        {
                          "key": {
                            "hash": {
                              "from": "bcedc450f8481e89b1445069acdc3dd9",
                              "to": "d41d8cd98f00b204e9800998ecf8427e"
                            }
                          },
                          "value": {
                            "hash": {
                              "from": "b326b5062b2f0e69046810717534cb09",
                              "to": "d41d8cd98f00b204e9800998ecf8427e"
                            },
                            "type": "value"
                          }
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "4101bef8794fed986e95dfb54850c68b": "nope"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change simple value to array`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("change simple value to array")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "4ec31704a3ec981be364071f6b6c9adc",
                  "to": "e3c38f42d8eca719ae2b293a518a2ba9"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "4101bef8794fed986e95dfb54850c68b",
                        "to": "f1f713c9e000f5d3f280adbd124df4f5"
                      },
                      "type": {
                        "from": "value",
                        "to": "array"
                      },
                      "children": []
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change simple value to array with values`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        val after = """
            {
              "bob": ["nope"]
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("change simple value to array with values")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "4ec31704a3ec981be364071f6b6c9adc",
                  "to": "b3cda3d23485d5959a15561a7aa4b0f3"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "4101bef8794fed986e95dfb54850c68b",
                        "to": "9b0ecb4117ce32422ff0366734ad16b2"
                      },
                      "type": {
                        "from": "value",
                        "to": "array"
                      },
                      "children": [
                        {
                          "hash": {
                            "from": "d41d8cd98f00b204e9800998ecf8427e",
                            "to": "4101bef8794fed986e95dfb54850c68b"
                          },
                          "type": "value"
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "4101bef8794fed986e95dfb54850c68b": "nope"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change array to simple value`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("change array to simple value")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "e3c38f42d8eca719ae2b293a518a2ba9",
                  "to": "4ec31704a3ec981be364071f6b6c9adc"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "f1f713c9e000f5d3f280adbd124df4f5",
                        "to": "4101bef8794fed986e95dfb54850c68b"
                      },
                      "children": [],
                      "type": {
                        "from": "array",
                        "to": "value"
                      }
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "4101bef8794fed986e95dfb54850c68b": "nope"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change array with value to simple value`() {
        val before = """
            {
              "bob": [
                "wow"
              ]
            }
        """.toJsonObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("change array with value to simple value")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "0c2c0a8bd2988b7a69b90723538ce423",
                  "to": "4ec31704a3ec981be364071f6b6c9adc"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "59dda4f77217ec8b40445ef72194fd3b",
                        "to": "4101bef8794fed986e95dfb54850c68b"
                      },
                      "type": {
                        "from": "array",
                        "to": "value"
                      },
                      "children": [
                        {
                          "hash": {
                            "from": "bcedc450f8481e89b1445069acdc3dd9",
                            "to": "d41d8cd98f00b204e9800998ecf8427e"
                          },
                          "type": "value"
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "4101bef8794fed986e95dfb54850c68b": "nope"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change object to array`() {
        val before = """
            {
              "bob": {"sup":true}
            }
        """.toJsonObject()
        val after = """
            {
              "bob": ["no"]
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("change object to array")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "9b49796db5ddf5b6ed7f4fd85bc6f6e8",
                  "to": "7635887fb714d63ebc8aa35417c7bd45"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "8e091c9f03fdb31faba1e7523a766dcf",
                        "to": "0679df3a0e3c3ef2afeea91897161f1c"
                      },
                      "type": {
                        "from": "object",
                        "to": "array"
                      },
                      "children": [
                        {
                          "key": {
                            "hash": {
                              "from": "2eeecd72c567401e6988624b179d0b14",
                              "to": "d41d8cd98f00b204e9800998ecf8427e"
                            }
                          },
                          "value": {
                            "hash": {
                              "from": "b326b5062b2f0e69046810717534cb09",
                              "to": "d41d8cd98f00b204e9800998ecf8427e"
                            },
                            "type": "value"
                          }
                        },
                        {
                          "hash": {
                            "from": "d41d8cd98f00b204e9800998ecf8427e",
                            "to": "7fa3b767c460b54a2be4d49030b349c7"
                          },
                          "type": "value"
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "7fa3b767c460b54a2be4d49030b349c7": "no"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change array to object`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonObject()
        val after = """
            {
              "bob": {}
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("change array to object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "e3c38f42d8eca719ae2b293a518a2ba9",
                  "to": "e6cf1d2f3db117c3813f0cc8ff3dc2e0"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "f1f713c9e000f5d3f280adbd124df4f5",
                        "to": "a8cfde6331bd59eb2ac96f8911c4b666"
                      },
                      "type": {
                        "from": "array",
                        "to": "object"
                      },
                      "children": []
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `change array (with values) to object (with values)`() {
        val before = """
            {
              "bob": ["nope"]
            }
        """.toJsonObject()
        val after = """
            {
              "bob": {"yep": true}
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("change array (with values) to object (with values)")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "b3cda3d23485d5959a15561a7aa4b0f3",
                  "to": "05c9a23b2fa5f8ccda9c2ce3f072082f"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "9b0ecb4117ce32422ff0366734ad16b2",
                        "to": "2188064150411fa5da0a0f5c166d7917"
                      },
                      "type": {
                        "from": "array",
                        "to": "object"
                      },
                      "children": [
                        {
                          "hash": {
                            "from": "4101bef8794fed986e95dfb54850c68b",
                            "to": "d41d8cd98f00b204e9800998ecf8427e"
                          },
                          "type": "value"
                        },
                        {
                          "key": {
                            "hash": {
                              "from": "d41d8cd98f00b204e9800998ecf8427e",
                              "to": "9348ae7851cf3ba798d9564ef308ec25"
                            }
                          },
                          "value": {
                            "hash": {
                              "from": "d41d8cd98f00b204e9800998ecf8427e",
                              "to": "b326b5062b2f0e69046810717534cb09"
                            },
                            "type": "value"
                          }
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "9348ae7851cf3ba798d9564ef308ec25": "yep",
                "b326b5062b2f0e69046810717534cb09": true
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `add object to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonObject()
        val after = """
            {
              "bob": [{}]
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("add object to array")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "e3c38f42d8eca719ae2b293a518a2ba9",
                  "to": "675152b84d5249a416f3969951e2d121"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "f1f713c9e000f5d3f280adbd124df4f5",
                        "to": "64eca22170f35eb251066a19bb7388eb"
                      },
                      "type": "array",
                      "children": [
                        {
                          "hash": {
                            "from": "d41d8cd98f00b204e9800998ecf8427e",
                            "to": "a8cfde6331bd59eb2ac96f8911c4b666"
                          },
                          "type": "object",
                          "children": []
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `remove object from array`() {
        val before = """
            {
              "bob": [{}]
            }
        """.toJsonObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("remove object from array")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "675152b84d5249a416f3969951e2d121",
                  "to": "e3c38f42d8eca719ae2b293a518a2ba9"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "64eca22170f35eb251066a19bb7388eb",
                        "to": "f1f713c9e000f5d3f280adbd124df4f5"
                      },
                      "type": "array",
                      "children": [
                        {
                          "hash": {
                            "from": "a8cfde6331bd59eb2ac96f8911c4b666",
                            "to": "d41d8cd98f00b204e9800998ecf8427e"
                          },
                          "type": "object",
                          "children": []
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `add array to object`() {
        val before = "{}".toJsonObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("add array to object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "a8cfde6331bd59eb2ac96f8911c4b666",
                  "to": "e3c38f42d8eca719ae2b293a518a2ba9"
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
                        "to": "f1f713c9e000f5d3f280adbd124df4f5"
                      },
                      "type": "array",
                      "children": []
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "9f9d51bc70ef21ca5c14f307980a29d8": "bob"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `remove array from object`() {
        val before = """
            {
              "bob": ["nope"]
            }
        """.toJsonObject()
        val after = "{}".toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("remove array from object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "b3cda3d23485d5959a15561a7aa4b0f3",
                  "to": "a8cfde6331bd59eb2ac96f8911c4b666"
                },
                "children": [
                  {
                    "key": {
                      "hash": {
                        "from": "9f9d51bc70ef21ca5c14f307980a29d8",
                        "to": "d41d8cd98f00b204e9800998ecf8427e"
                      }
                    },
                    "value": {
                      "hash": {
                        "from": "9b0ecb4117ce32422ff0366734ad16b2",
                        "to": "d41d8cd98f00b204e9800998ecf8427e"
                      },
                      "type": "array",
                      "children": [
                        {
                          "hash": {
                            "from": "4101bef8794fed986e95dfb54850c68b",
                            "to": "d41d8cd98f00b204e9800998ecf8427e"
                          },
                          "type": "value"
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `add simple value to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonObject()

        val after = """
            {
              "bob": [
                "hello"
              ]
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("add simple value to array")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "e3c38f42d8eca719ae2b293a518a2ba9",
                  "to": "ffa96327843d20417a8ea93f72cf002a"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "f1f713c9e000f5d3f280adbd124df4f5",
                        "to": "183c6f6bc203c6097b5d565bf43efcb9"
                      },
                      "type": "array",
                      "children": [
                        {
                          "hash": {
                            "from": "d41d8cd98f00b204e9800998ecf8427e",
                            "to": "5d41402abc4b2a76b9719d911017c592"
                          },
                          "type": "value"
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "5d41402abc4b2a76b9719d911017c592": "hello"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `remove simple value from array`() {
        val before = """
            {
              "bob": [
                "hello"
              ]
            }
        """.toJsonObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("remove simple value from array")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "ffa96327843d20417a8ea93f72cf002a",
                  "to": "e3c38f42d8eca719ae2b293a518a2ba9"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "183c6f6bc203c6097b5d565bf43efcb9",
                        "to": "f1f713c9e000f5d3f280adbd124df4f5"
                      },
                      "type": "array",
                      "children": [
                        {
                          "hash": {
                            "from": "5d41402abc4b2a76b9719d911017c592",
                            "to": "d41d8cd98f00b204e9800998ecf8427e"
                          },
                          "type": "value"
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `add array to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonObject()
        val after = """
            {
              "bob": [[]]
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("add array to array")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "e3c38f42d8eca719ae2b293a518a2ba9",
                  "to": "9fac993e0abffefb07b5975571becbf0"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "f1f713c9e000f5d3f280adbd124df4f5",
                        "to": "23fd6bf3a9d8a6e2ea7b78cc7844f859"
                      },
                      "type": "array",
                      "children": [
                        {
                          "hash": {
                            "from": "d41d8cd98f00b204e9800998ecf8427e",
                            "to": "f1f713c9e000f5d3f280adbd124df4f5"
                          },
                          "type": "array",
                          "children": []
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `remove array from array`() {
        val before = """
            {
              "bob": [["wow"]]
            }
        """.toJsonObject()
        val after = """
            {
              "bob": []
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("remove array from array")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "1da17f77bd78eb2de76c3f21166851ec",
                  "to": "e3c38f42d8eca719ae2b293a518a2ba9"
                },
                "children": [
                  {
                    "key": {
                      "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                    },
                    "value": {
                      "hash": {
                        "from": "d01c493627660b2e3b79cea269160b6c",
                        "to": "f1f713c9e000f5d3f280adbd124df4f5"
                      },
                      "type": "array",
                      "children": [
                        {
                          "hash": {
                            "from": "59dda4f77217ec8b40445ef72194fd3b",
                            "to": "d41d8cd98f00b204e9800998ecf8427e"
                          },
                          "children": [
                            {
                              "hash": {
                                "from": "bcedc450f8481e89b1445069acdc3dd9",
                                "to": "d41d8cd98f00b204e9800998ecf8427e"
                              },
                              "type": "value"
                            }
                          ],
                          "type": "array"
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `add object to object`() {
        val before = "{}".toJsonObject()

        val after = """
            {
              "bob": {}
            }
        """.toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("add object to object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "a8cfde6331bd59eb2ac96f8911c4b666",
                  "to": "e6cf1d2f3db117c3813f0cc8ff3dc2e0"
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
                        "to": "a8cfde6331bd59eb2ac96f8911c4b666"
                      },
                      "type": "object",
                      "children": []
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "9f9d51bc70ef21ca5c14f307980a29d8": "bob"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `remove object from object`() {
        val before = """
            {
              "bob": {}
            }
        """.toJsonObject()

        val after = "{}".toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("remove object from object")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "e6cf1d2f3db117c3813f0cc8ff3dc2e0",
                  "to": "a8cfde6331bd59eb2ac96f8911c4b666"
                },
                "children": [
                  {
                    "key": {
                      "hash": {
                        "from": "9f9d51bc70ef21ca5c14f307980a29d8",
                        "to": "d41d8cd98f00b204e9800998ecf8427e"
                      }
                    },
                    "value": {
                      "hash": {
                        "from": "a8cfde6331bd59eb2ac96f8911c4b666",
                        "to": "d41d8cd98f00b204e9800998ecf8427e"
                      },
                      "type": "object",
                      "children": []
                    }
                  }
                ],
                "type": "object"
              },
              "values": {}
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )
    }

    @Test
    fun `nested object change`() {
        val before = """
            {
              "hello": "wow",
              "child": {
                "nope": "yep"
              }
            }
            """.trimIndent().toJsonObject()

        val after = """
            {
              "hello": "wow",
              "child": {
                "nope": "yep",
                "wowza": "thing"
              }
            }
        """.trimIndent().toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("nested object change")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "b6ab248bb2ab1ad41ef359528b7764f5",
                  "to": "c8a53cd78536ff6de4023d3214195f16"
                },
                "children": [
                  {
                    "key": {
                      "hash": "1b7d5726533ab525a8760351e9b5e415"
                    },
                    "value": {
                      "hash": {
                        "from": "b4d31e193668ccf266ccd6a67538a85f",
                        "to": "1e3cface34160d67450a28bcd45e8052"
                      },
                      "type": "object",
                      "children": [
                        {
                          "key": {
                            "hash": {
                              "from": "d41d8cd98f00b204e9800998ecf8427e",
                              "to": "0b0c1647f9c38d9e0a510108fbad18c1"
                            }
                          },
                          "value": {
                            "hash": {
                              "from": "d41d8cd98f00b204e9800998ecf8427e",
                              "to": "8afc1e9bec810034dafd45c6854f1dd9"
                            },
                            "type": "value"
                          }
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "0b0c1647f9c38d9e0a510108fbad18c1": "wowza",
                "8afc1e9bec810034dafd45c6854f1dd9": "thing"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )

    }

    @Test
    fun `nested array change`() {
        val before = """
            {
              "hello": "wow",
              "child": [
                {
                  "nope": "yep"
                }
              ]
            }
            """.trimIndent().toJsonObject()

        val after = """
            {
              "hello": "wow",
              "child": [
                {
                  "nope": {}
                }
              ]
            }
        """.trimIndent().toJsonObject()

        // loading data into keymap
        val document = Plinko.collection("testFormat").document("nested array change")
        document.setData(after)

        val diff = DiffGenerator.getDiff(
            first = JsonParser.read(before).toHashObject(),
            second = JsonParser.read(after).toHashObject()
        )

        val commit = Commit(55555555555, commitTimeCreatedAt, diff).also {
            println(it.asJson())
        }

        val expected = """
            {
              "documentId": 55555555555,
              "createdAt": "2019-01-01T01:01:01.001Z",
              "diff": {
                "hash": {
                  "from": "c6892c411d933af8556a8f39e010c3ac",
                  "to": "ace78d907a16c7bc479469ec6563da92"
                },
                "children": [
                  {
                    "key": {
                      "hash": "1b7d5726533ab525a8760351e9b5e415"
                    },
                    "value": {
                      "hash": {
                        "from": "98b0a7adec34644f68b77353ac6c673a",
                        "to": "5a2843b5162a11fe9ed308ab1dd47749"
                      },
                      "type": "array",
                      "children": [
                        {
                          "hash": {
                            "from": "d41d8cd98f00b204e9800998ecf8427e",
                            "to": "aa758697df4fd5990454ba04b8bc6fbe"
                          },
                          "children": [
                            {
                              "key": {
                                "hash": {
                                  "from": "d41d8cd98f00b204e9800998ecf8427e",
                                  "to": "4101bef8794fed986e95dfb54850c68b"
                                }
                              },
                              "value": {
                                "hash": {
                                  "from": "d41d8cd98f00b204e9800998ecf8427e",
                                  "to": "a8cfde6331bd59eb2ac96f8911c4b666"
                                },
                                "children": [],
                                "type": "object"
                              }
                            }
                          ],
                          "type": "object"
                        },
                        {
                          "hash": {
                            "from": "b4d31e193668ccf266ccd6a67538a85f",
                            "to": "d41d8cd98f00b204e9800998ecf8427e"
                          },
                          "children": [
                            {
                              "key": {
                                "hash": {
                                  "from": "4101bef8794fed986e95dfb54850c68b",
                                  "to": "d41d8cd98f00b204e9800998ecf8427e"
                                }
                              },
                              "value": {
                                "hash": {
                                  "from": "9348ae7851cf3ba798d9564ef308ec25",
                                  "to": "d41d8cd98f00b204e9800998ecf8427e"
                                },
                                "type": "value"
                              }
                            }
                          ],
                          "type": "object"
                        }
                      ]
                    }
                  }
                ],
                "type": "object"
              },
              "values": {
                "4101bef8794fed986e95dfb54850c68b": "nope"
              }
            }
        """.toJsonObject()

        Assert.assertEquals(
            expected,
            DiffTransportFormatter.format(commit)
        )

    }
}
