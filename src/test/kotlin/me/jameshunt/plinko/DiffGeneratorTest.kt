package me.jameshunt.plinko

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.merkle.*
import org.junit.Assert
import org.junit.Test

fun String.toDiffJson(): DiffParser.ValueInfo = ObjectMapper()
    .readValue<Map<String, Any>>(this, object : TypeReference<Map<String, Any>>() {})
    .let { DiffParser.parseDiff(it) }

fun String.toJsonHashObject(): HashObject = ObjectMapper()
    .readValue<Map<String, Any?>>(this, object : TypeReference<Map<String, Any?>>() {})
    .let { JsonParser.read(it) }
    .toHashObject()

class DiffGeneratorTest {

    @Test
    fun `add key-value to object`() {
        val before = "{}".toJsonHashObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
              "type": "object",
              "hash": {
                "from": "d41d8cd98f00b204e9800998ecf8427e",
                "to": "fc824db5d986fa9dbb2a1860d89a84c4"
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
                    "type": "value",
                    "hash": {
                      "from": "d41d8cd98f00b204e9800998ecf8427e",
                      "to": "4101bef8794fed986e95dfb54850c68b"
                    }
                  }
                }
              ]
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `remove key-value from object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()
        val after = "{}".toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
              "type": "object",
              "hash": {
                "from": "fc824db5d986fa9dbb2a1860d89a84c4",
                "to": "d41d8cd98f00b204e9800998ecf8427e"
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
                    "type": "value",
                    "hash": {
                      "from": "4101bef8794fed986e95dfb54850c68b",
                      "to": "d41d8cd98f00b204e9800998ecf8427e"
                    }
                  }
                }
              ]
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change key in object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "susan": "nope"
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
              "type": "object",
              "hash": {
                "from": "fc824db5d986fa9dbb2a1860d89a84c4",
                "to": "258779ca69e004985df6b67af0204b0d"
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
              ]
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change simple value to different simple value`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": "yep"
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
              "type": "object",
              "hash": {
                "from": "fc824db5d986fa9dbb2a1860d89a84c4",
                "to": "de8c4672ad26455ab87e857155574b98"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "type": "value",
                    "hash": {
                      "from": "4101bef8794fed986e95dfb54850c68b",
                      "to": "9348ae7851cf3ba798d9564ef308ec25"
                    }
                  }
                }
              ]
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change simple value to object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": {
                "wow": true
              }
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
              "type": "object",
              "hash": {
                "from": "fc824db5d986fa9dbb2a1860d89a84c4",
                "to": "55b879d5995f451e280b84f1a81f731a"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "type": "object",
                    "hash": {
                      "from": "4101bef8794fed986e95dfb54850c68b",
                      "to": "873e8c183f74eed98d08f5ec2c2832e9"
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
                          "type": "value",
                          "hash":{
                            "from":"d41d8cd98f00b204e9800998ecf8427e",
                            "to":"b326b5062b2f0e69046810717534cb09"
                          }
                        }
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change simple value to array`() {
        TODO()
    }

    @Test
    fun `change object to simple value`() {
        val before = """
            {
              "bob": {
                "wow": true
              }
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
              "hash": {
                "from": "55b879d5995f451e280b84f1a81f731a",
                "to": "fc824db5d986fa9dbb2a1860d89a84c4"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "873e8c183f74eed98d08f5ec2c2832e9",
                      "to": "4101bef8794fed986e95dfb54850c68b"
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
                    ],
                    "type": "value"
                  }
                }
              ],
              "type": "object"
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change array to simple value`() {
        TODO()
    }

    @Test
    fun `change object to array`() {
        TODO()
    }

    @Test
    fun `change array to object`() {
        TODO()
    }

    @Test
    fun `add simple value to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": [
                "hello"
              ]
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
              "hash": {
                "from": "3f7c9f23d16d571114ff34ab2adff983",
                "to": "4258bd7cd50f560ae980ee3bcb54a394"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "d41d8cd98f00b204e9800998ecf8427e",
                      "to": "69a329523ce1ec88bf63061863d9cb14"
                    },
                    "children": [
                      {
                        "hash": {
                          "from": "d41d8cd98f00b204e9800998ecf8427e",
                          "to": "5d41402abc4b2a76b9719d911017c592"
                        },
                        "type": "value"
                      }
                    ],
                    "type": "array"
                  }
                }
              ],
              "type": "object"
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `add object to array`() {
        TODO()
    }

    @Test
    fun `add array to object`() {
        val before = "{}".toJsonHashObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        //TODO: REMEMBER value hashes of new empty array for parsing
        val expectedDiff = """
            {
              "hash": {
                "from": "d41d8cd98f00b204e9800998ecf8427e",
                "to": "3f7c9f23d16d571114ff34ab2adff983"
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
                      "to": "d41d8cd98f00b204e9800998ecf8427e"
                    },
                    "children": [],
                    "type": "array"
                  }
                }
              ],
              "type": "object"
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `add array to array`() {
        TODO()
    }

    @Test
    fun `remove simple value from array`() {
        TODO()
    }

    @Test
    fun `remove object from array`() {
        TODO()
    }

    @Test
    fun `remove array from array`() {
        TODO()
    }
}
