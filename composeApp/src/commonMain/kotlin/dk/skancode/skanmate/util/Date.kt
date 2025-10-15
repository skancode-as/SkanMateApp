package dk.skancode.skanmate.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun currentDateTimeUTC(): LocalDateTime {
    val now: Instant = Clock.System.now()
    return now.toLocalDateTime(TimeZone.UTC)
}

fun LocalDateTime.formatISO(): String {
    return this.format(format = LocalDateTime.Formats.ISO)
}