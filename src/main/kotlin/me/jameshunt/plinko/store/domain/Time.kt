package me.jameshunt.plinko.store.domain

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

val now: String
    get() = OffsetDateTime.now().format()

fun OffsetDateTime.format(): String = this.format(DateTimeFormatter.ISO_INSTANT)
