@file:OptIn(ExperimentalTime::class, FormatStringsInDatetimeFormats::class)

package dk.skancode.skanmate.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.offsetIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun currentDateTimeUTC(): LocalDateTime {
    val now: Instant = Clock.System.now()
    return now.toLocalDateTime(TimeZone.UTC)
}

fun LocalDateTime.formatISO(): String {
    return this.format(format = LocalDateTime.Formats.ISO)
}

fun String.toLocalTimeString(): String {
    val tz = TimeZone.currentSystemDefault()
    val instant = this.isoToUTCInstant()
    val offset = instant.offsetIn(tz)

    return instant
        .toLocalDateTime(offset.asTimeZone())
        .format("yyyy-MM-dd HH:mm:ss")
}

fun String.isoToDateTime(): LocalDateTime {
    return LocalDateTime.parse(this, format = LocalDateTime.Formats.ISO)
}

fun String.isoToUTCInstant(): Instant {
    return isoToDateTime().toInstant(TimeZone.UTC)
}

fun LocalDateTime.format(pattern: String): String {
    return this.format(format = LocalDateTime.Format {
        byUnicodePattern(pattern = pattern)
    })
}