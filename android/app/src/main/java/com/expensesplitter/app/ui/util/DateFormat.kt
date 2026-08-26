package com.expensesplitter.app.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("MMM d")

// Formats a backend ISO-8601 timestamp for display: "Today, 3:19 PM" if it's
// today, otherwise "Aug 20". Falls back to the raw string if unparseable.
fun formatActivityTimestamp(iso: String): String = try {
    val instant = Instant.parse(iso)
    val zone = ZoneId.systemDefault()
    val date = instant.atZone(zone)
    val today = Instant.now().atZone(zone)
    if (date.toLocalDate() == today.toLocalDate()) {
        "Today, ${date.format(DateTimeFormatter.ofPattern("h:mm a"))}"
    } else {
        date.format(dayFormatter)
    }
} catch (e: Exception) {
    iso
}
