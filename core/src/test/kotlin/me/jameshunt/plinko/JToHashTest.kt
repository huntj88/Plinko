package me.jameshunt.plinko

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.merkle.toHashObject
import org.junit.Assert
import org.junit.Test

class JToHashTest {

    @Test
    fun `print hash object as json`() {
       tree3
           .toHashObject()
           .let { ObjectMapper().writeValueAsString(it) }
           .let(::println)
    }

    @Test
    fun `test JObject to HashObject`() {

        val hashObject = tree9.toHashObject().let { ObjectMapper().writeValueAsString(it) }.also { println(it) }.let {
            val type = object : TypeReference<Map<String, Any>>() {}
            ObjectMapper().readValue<Map<String, Any>>(it, type)
        }

        val expected = """
        {
          "hash": "b5c4b63fadb49d51abcdfb1502f2ddef",
          "hobject": {
            "09f33e9079ba8f01ba1c219fb294e676": {
              "hash": "95bf73d42a60f9493f0616b74ddc5092",
              "harray": [
                {
                  "hash": "2eeecd72c567401e6988624b179d0b14"
                },
                {
                  "hash": "74e8333ad11685ff3bdae589c8f6e34d"
                },
                {
                  "hash": "bcedc450f8481e89b1445069acdc3dd9"
                }
              ]
            },
            "c13d88cb4cb02003daedb8a84e5d272a": {
              "hash": "f2b9804a2eed81872298d44bcaf7fb3c",
              "hobject": {
                "b068931cc450442b63f5b3d276ea4297": {
                  "hash": "d749904cbebce14f5612e4e600c48174"
                },
                "3c6e0b8a9c15224a8228b9a98ca1531d": {
                  "hash": "2063c1608d6e0baf80249c42e2be5804"
                },
                "4bdb23fce4d3c6b21f8b8e3c913f7cf9": {
                  "hash": "b326b5062b2f0e69046810717534cb09"
                },
                "9fb81d8f33d6f85c11c81aa6a45b40ba": {
                  "hash": "993a3ae7f396c0eb49ef1cfc78e97955",
                  "hobject": {
                    "4cdf5a25d4673bfc4546ca7843071f65": {
                      "hash": "5448223ad8d7a8fca661b406c750bde7"
                    },
                    "85b8a5f948a4195f482ab1c477ce4cfd": {
                      "hash": "de521a8e8f8b5e826485e8d5b2b9ca5f",
                      "harray": [
                        {
                          "hash": "63cc6b205c1862c5da7adc8446f63221",
                          "hobject": {
                            "13b73edae8443990be1aa8f1a483bc27": {
                              "hash": "b326b5062b2f0e69046810717534cb09"
                            }
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
          }
        }           
        """.trimIndent().let {
            val type = object : TypeReference<Map<String, Any>>() {}
            ObjectMapper().readValue<Map<String, Any>>(it, type)
        }

        Assert.assertEquals(expected, hashObject)
    }
}
