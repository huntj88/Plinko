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
    fun `no diff`() {
        val json = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(json, json).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
              "hash": {
                "from": "e3c38f42d8eca719ae2b293a518a2ba9",
                "to": "e3c38f42d8eca719ae2b293a518a2ba9"
              },
              "children": [],
              "type": "object"
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `add key-simpleValue to object`() {
        val before = "{}".toJsonHashObject()

        val after = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `remove key-simpleValue from object`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()
        val after = "{}".toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change simple value to array`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
            }
            """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change simple value to array with values`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
            }
            """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change array to simple value`() {
        val before = """
            {
              "bob": []
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
                    "type": {
                      "from": "array",
                      "to": "value"
                    },
                    "children": []
                  }
                }
              ],
              "type": "object"
            }
            """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change array with value to simple value`() {
        val before = """
            {
              "bob": [
                "wow"
              ]
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
            }
            """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change object to array`() {
        val before = """
            {
              "bob": {"sup":true}
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": ["no"]
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change array to object`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
                    "children": [],
                    "type": {
                      "from": "array",
                      "to": "object"
                    }
                  }
                }
              ],
              "type": "object"
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `change array (with values) to object (with values)`() {
        val before = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": {"yep": true}
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
                    ],
                    "type": {
                      "from": "array",
                      "to": "object"
                    }
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
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": [{}]
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
                        "children": [],
                        "type": "object"
                      }
                    ]
                  }
                }
              ],
              "type": "object"
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `remove object from array`() {
        val before = """
            {
              "bob": [{}]
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
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

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `remove array from object`() {
        val before = """
            {
              "bob": ["nope"]
            }
        """.toJsonHashObject()
        val after = "{}".toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `remove simple value from array`() {
        val before = """
            {
              "bob": [
                "hello"
              ]
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `add array to array`() {
        val before = """
            {
              "bob": []
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": [[]]
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `remove array from array`() {
        val before = """
            {
              "bob": [["wow"]]
            }
        """.toJsonHashObject()
        val after = """
            {
              "bob": []
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
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
                        "type": "array",
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
                    ]
                  }
                }
              ],
              "type": "object"
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `add object to object`() {
        val before = "{}".toJsonHashObject()

        val after = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }

    @Test
    fun `remove object from object`() {
        val before = """
            {
              "bob": {}
            }
        """.toJsonHashObject()

        val after = "{}".toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }

        val expectedDiff = """
            {
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
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }
}
