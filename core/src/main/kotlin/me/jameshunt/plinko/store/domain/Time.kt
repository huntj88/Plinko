package me.jameshunt.plinko.store.domain

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatterBuilder

val now: String
    get() = OffsetDateTime.now().format()

fun OffsetDateTime.format(): String = this.format(
    DateTimeFormatterBuilder()
        .appendInstant(3)
        .toFormatter()
)
