package com.sliide.usermanager.domain.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun Instant.toRelativeString(): String {
    val diff = Clock.System.now() - this
    if (diff.isNegative()) return "Just now"
    val mins  = diff.inWholeMinutes
    val hours = diff.inWholeHours
    val days  = diff.inWholeDays
    val weeks = days / 7
    return when {
        diff.inWholeSeconds < 60 -> "Just now"
        mins  < 60  -> if (mins  == 1L) "1 minute ago"  else "$mins minutes ago"
        hours < 24  -> if (hours == 1L) "1 hour ago"    else "$hours hours ago"
        days  < 7   -> if (days  == 1L) "1 day ago"     else "$days days ago"
        else        -> if (weeks == 1L) "1 week ago"    else "$weeks weeks ago"
    }
}
