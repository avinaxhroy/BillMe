package com.billme.app.util

import kotlinx.datetime.Instant

/**
 * Small helpers to interoperate between java.time and kotlinx.datetime
 * Using java.time here avoids depending on specific kotlinx.datetime extensions
 * that might have changed between versions.
 */
object DateTimeUtils {

    /**
     * Returns a kotlinx.datetime.Instant representing the start-of-day (00:00) in system zone for
     * (today - days).
     */
    fun startOfDayInstantDaysAgo(days: Int): Instant {
        val zone = java.time.ZoneId.systemDefault()
        val localDate = java.time.LocalDate.now(zone).minusDays(days.toLong())
        val zonedDateTime = localDate.atStartOfDay(zone)
        val epochMillis = zonedDateTime.toInstant().toEpochMilli()
        return Instant.fromEpochMilliseconds(epochMillis)
    }

    /**
     * Convert a java.time.Instant to kotlinx.datetime.Instant
     */
    fun fromJavaInstant(javaInstant: java.time.Instant): Instant = Instant.fromEpochMilliseconds(javaInstant.toEpochMilli())

    /**
     * Convert a kotlinx.datetime.Instant to java.time.Instant
     */
    fun toJavaInstant(kxInstant: Instant): java.time.Instant = java.time.Instant.ofEpochMilli(kxInstant.toEpochMilliseconds())

    /**
     * Convert a kotlinx.datetime.LocalDate (start of day in system zone) to kotlinx.datetime.Instant
     */
    fun fromKxLocalDateStartOfDay(localDate: kotlinx.datetime.LocalDate): Instant {
        val zone = java.time.ZoneId.systemDefault()
        val jLocalDate = java.time.LocalDate.of(localDate.year, localDate.monthNumber, localDate.dayOfMonth)
        val zonedDateTime = jLocalDate.atStartOfDay(zone)
        val epochMillis = zonedDateTime.toInstant().toEpochMilli()
        return Instant.fromEpochMilliseconds(epochMillis)
    }

    /**
     * Convert a kotlinx.datetime.LocalDateTime (in system zone) to kotlinx.datetime.Instant
     */
    fun fromKxLocalDateTime(localDateTime: kotlinx.datetime.LocalDateTime): Instant {
        val zone = java.time.ZoneId.systemDefault()
        val jldt = java.time.LocalDateTime.of(
            localDateTime.year,
            localDateTime.monthNumber,
            localDateTime.dayOfMonth,
            localDateTime.hour,
            localDateTime.minute,
            localDateTime.second,
            localDateTime.nanosecond
        )
        val epochMillis = jldt.atZone(zone).toInstant().toEpochMilli()
        return Instant.fromEpochMilliseconds(epochMillis)
    }

    /**
     * Add hours to a kotlinx Instant using java.time arithmetic and return a new kotlinx Instant
     */
    fun addHours(kxInstant: Instant, hours: Long): Instant {
        val ji = toJavaInstant(kxInstant)
        val added = ji.plus(java.time.Duration.ofHours(hours))
        return fromJavaInstant(added)
    }

    /**
     * Add days to a kotlinx Instant using java.time arithmetic and return a new kotlinx Instant
     */
    fun addDays(kxInstant: Instant, days: Long): Instant {
        val ji = toJavaInstant(kxInstant)
        val added = ji.plus(java.time.Duration.ofDays(days))
        return fromJavaInstant(added)
    }
}
