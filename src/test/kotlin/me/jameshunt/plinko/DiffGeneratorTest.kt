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
                    "type": {
                      "from": "object",
                      "to": "value"
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
    fun `change simple value to array`() {
        val before = """
            {
              "bob": "nope"
            }
        """.toJsonHashObject()

        val after = """
            {
              "bob": [
                "wow"
              ]
            }
        """.toJsonHashObject()

        val actual = DiffGenerator.getDiff(before, after).let { DiffParser.parseDiff(it) }
        println(actual)

        val expectedDiff = """
            {
              "hash": {
                "from": "fc824db5d986fa9dbb2a1860d89a84c4",
                "to": "776f5e468405ba973c11b750dc2d85b9"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "4101bef8794fed986e95dfb54850c68b",
                      "to": "96cabca7a92e0ebe17f802ad6e592cb2"
                    },
                    "children": [
                      {
                        "hash": {
                          "from": "d41d8cd98f00b204e9800998ecf8427e",
                          "to": "bcedc450f8481e89b1445069acdc3dd9"
                        },
                        "type": "value"
                      }
                    ],
                    "type": {
                      "from": "value",
                      "to": "array"
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
    fun `change array to simple value`() {
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
                "from": "776f5e468405ba973c11b750dc2d85b9",
                "to": "fc824db5d986fa9dbb2a1860d89a84c4"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "96cabca7a92e0ebe17f802ad6e592cb2",
                      "to": "4101bef8794fed986e95dfb54850c68b"
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
                    "type": {
                      "from": "array",
                      "to": "value"
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
                "from": "abcd124b36511a5af62422490e5d13d9",
                "to": "c1815e107c43c151c0612628f220eace"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "4f2d996ad7ead823e6a6e73ffa074b1d",
                      "to": "35c848caf5eba1d2551d2f1a67cf7618"
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
                    ],
                    "type": {
                      "from": "object",
                      "to": "array"
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
                "from": "3f7c9f23d16d571114ff34ab2adff983",
                "to": "bd35717065820c2a8cf26f303f888e61"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "d41d8cd98f00b204e9800998ecf8427e",
                      "to": "74be16979710d4c4e7c6647856088456"
                    },
                    "children": [
                      {
                        "hash": {
                          "from": "d41d8cd98f00b204e9800998ecf8427e",
                          "to": "d41d8cd98f00b204e9800998ecf8427e"
                        },
                        "type": "object",
                        "children": []
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
                "from": "bd35717065820c2a8cf26f303f888e61",
                "to": "3f7c9f23d16d571114ff34ab2adff983"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "74be16979710d4c4e7c6647856088456",
                      "to": "d41d8cd98f00b204e9800998ecf8427e"
                    },
                    "children": [
                      {
                        "hash": {
                          "from": "d41d8cd98f00b204e9800998ecf8427e",
                          "to": "d41d8cd98f00b204e9800998ecf8427e"
                        },
                        "children": [],
                        "type": "object"
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
                "from": "04922aa55d3278b0dcbb7aafcfab1f09",
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
                    "hash": {
                      "from": "79c30748b84bd4f0fe1ddbc3dcd72969",
                      "to": "d41d8cd98f00b204e9800998ecf8427e"
                    },
                    "children": [
                      {
                        "hash": {
                          "from": "4101bef8794fed986e95dfb54850c68b",
                          "to": "d41d8cd98f00b204e9800998ecf8427e"
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
                "from": "4258bd7cd50f560ae980ee3bcb54a394",
                "to": "3f7c9f23d16d571114ff34ab2adff983"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "69a329523ce1ec88bf63061863d9cb14",
                      "to": "d41d8cd98f00b204e9800998ecf8427e"
                    },
                    "children": [
                      {
                        "hash": {
                          "from": "5d41402abc4b2a76b9719d911017c592",
                          "to": "d41d8cd98f00b204e9800998ecf8427e"
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
                "from": "3f7c9f23d16d571114ff34ab2adff983",
                "to": "bd35717065820c2a8cf26f303f888e61"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "d41d8cd98f00b204e9800998ecf8427e",
                      "to": "74be16979710d4c4e7c6647856088456"
                    },
                    "children": [
                      {
                        "hash": {
                          "from": "d41d8cd98f00b204e9800998ecf8427e",
                          "to": "d41d8cd98f00b204e9800998ecf8427e"
                        },
                        "type": "array",
                        "children": []
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
                "from": "03f277fbc07274a23528733812e6f23d",
                "to": "3f7c9f23d16d571114ff34ab2adff983"
              },
              "children": [
                {
                  "key": {
                    "hash": "9f9d51bc70ef21ca5c14f307980a29d8"
                  },
                  "value": {
                    "hash": {
                      "from": "920d7d658c6acc9d7c8251ab50405db1",
                      "to": "d41d8cd98f00b204e9800998ecf8427e"
                    },
                    "children": [
                      {
                        "hash": {
                          "from": "96cabca7a92e0ebe17f802ad6e592cb2",
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
                    "type": "object"
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
                "from": "3f7c9f23d16d571114ff34ab2adff983",
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
                    "hash": {
                      "from": "d41d8cd98f00b204e9800998ecf8427e",
                      "to": "d41d8cd98f00b204e9800998ecf8427e"
                    },
                    "children": [],
                    "type": "object"
                  }
                }
              ],
              "type": "object"
            }
        """.trimIndent().toDiffJson()
        Assert.assertEquals(expectedDiff, actual)
    }
}
