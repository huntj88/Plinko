package me.jameshunt.plinko

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import me.jameshunt.plinko.merkle.JsonParser
import java.io.File

fun String.toJson(): Map<String, Any?> = ObjectMapper()
    .readValue<Map<String, Any?>>(this, object : TypeReference<Map<String, Any?>>() {})

val tree1 by lazy {
    File("src/test/resources/1.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}


val tree2 by lazy {
    File("src/test/resources/2.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}


val tree3 by lazy {
    File("src/test/resources/3.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}

val tree4 by lazy {
    File("src/test/resources/4.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}


val tree5 by lazy {
    File("src/test/resources/5.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}


val tree6 by lazy {
    File("src/test/resources/6.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}


val tree7 by lazy {
    File("src/test/resources/7.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}


val tree8 by lazy {
    File("src/test/resources/8.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}


val tree9 by lazy {
    File("src/test/resources/9.json")
        .readText()
        .toJson()
        .let { JsonParser.read(it) }
}
