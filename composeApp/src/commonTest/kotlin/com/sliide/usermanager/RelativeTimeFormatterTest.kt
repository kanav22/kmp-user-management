package com.sliide.usermanager

import com.sliide.usermanager.domain.util.toRelativeString
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RelativeTimeFormatterTest {

    private fun instantAgo(duration: kotlin.time.Duration): Instant = Clock.System.now() - duration

    @Test
    fun `future timestamp returns Just now`() {
        val future = Clock.System.now() + 60.seconds
        assertEquals("Just now", future.toRelativeString())
    }

    @Test
    fun `zero seconds returns Just now`() {
        assertEquals("Just now", Clock.System.now().toRelativeString())
    }

    @Test
    fun `59 seconds returns Just now`() {
        assertEquals("Just now", instantAgo(59.seconds).toRelativeString())
    }

    @Test
    fun `60 seconds returns 1 minute ago`() {
        assertEquals("1 minute ago", instantAgo(60.seconds).toRelativeString())
    }

    @Test
    fun `2 minutes returns 2 minutes ago`() {
        assertEquals("2 minutes ago", instantAgo(2.minutes).toRelativeString())
    }

    @Test
    fun `59 minutes returns 59 minutes ago`() {
        assertEquals("59 minutes ago", instantAgo(59.minutes).toRelativeString())
    }

    @Test
    fun `60 minutes returns 1 hour ago`() {
        assertEquals("1 hour ago", instantAgo(60.minutes).toRelativeString())
    }

    @Test
    fun `2 hours returns 2 hours ago`() {
        assertEquals("2 hours ago", instantAgo(2.hours).toRelativeString())
    }

    @Test
    fun `23 hours returns 23 hours ago`() {
        assertEquals("23 hours ago", instantAgo(23.hours).toRelativeString())
    }

    @Test
    fun `24 hours returns 1 day ago`() {
        assertEquals("1 day ago", instantAgo(24.hours).toRelativeString())
    }

    @Test
    fun `7 days returns 1 week ago`() {
        assertEquals("1 week ago", instantAgo(7.days).toRelativeString())
    }

    @Test
    fun `14 days returns 2 weeks ago`() {
        assertEquals("2 weeks ago", instantAgo(14.days).toRelativeString())
    }
}
