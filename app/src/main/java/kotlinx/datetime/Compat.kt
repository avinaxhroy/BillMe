package kotlinx.datetime

import com.billme.app.util.DateTimeUtils
import java.time.ZoneId

/**
 * Compatibility shims for older kotlinx.datetime APIs used in the codebase.
 * These delegate to java.time via DateTimeUtils where convenient so we can
 * migrate incrementally without changing all call sites at once.
 */

// Provide a top-level now() if code calls 'now()' unqualified
fun now(): Instant = Clock.System.now()

// Instant helpers
fun Instant.toEpochSecond(): Long = this.epochSeconds
val Instant.nanosecond: Int get() = this.nanosecondsOfSecond

// LocalDate helpers
fun LocalDate.atStartOfDayIn(timeZone: TimeZone): LocalDateTime {
    // Use java.time to compute start-of-day in system zone and convert to kotlinx LocalDateTime
    val zone = ZoneId.systemDefault()
    val jLocalDate = java.time.LocalDate.of(this.year, this.monthNumber, this.dayOfMonth)
    val jLdt = jLocalDate.atStartOfDay(zone).toLocalDateTime()
    return LocalDateTime(
        jLdt.year,
        jLdt.monthValue,
        jLdt.dayOfMonth,
        jLdt.hour,
        jLdt.minute,
        jLdt.second,
        jLdt.nano
    )
}

// LocalDateTime helpers
val LocalDateTime.monthValue: Int get() = this.monthNumber
val LocalDateTime.nano: Int get() = this.nanosecond

fun LocalDateTime.toEpochSecond(timeZone: TimeZone): Long {
    val zone = ZoneId.systemDefault()
    val jldt = java.time.LocalDateTime.of(this.year, this.monthNumber, this.dayOfMonth, this.hour, this.minute, this.second, this.nanosecond)
    return jldt.atZone(zone).toEpochSecond()
}

// Instant -> LocalDateTime conversion
fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime {
    val jInstant = DateTimeUtils.toJavaInstant(this)
    val zone = ZoneId.systemDefault()
    val jldt = java.time.LocalDateTime.ofInstant(jInstant, zone)
    return LocalDateTime(
        jldt.year,
        jldt.monthValue,
        jldt.dayOfMonth,
        jldt.hour,
        jldt.minute,
        jldt.second,
        jldt.nano
    )
}
