

package ch.pc.klbx.demo

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

private var globalClock: Clock = Clock.System
fun setClock(clock: Clock) {
    globalClock = clock
}

fun now(): Instant {
    return globalClock.now()
}

fun fromNow(timespan: Duration): Instant {
    return now().plus(timespan)
}

fun utcDateFromNow(timespan: Duration) : LocalDateTime {
    return fromNow(timespan).toLocalDateTime(TimeZone.UTC)
}