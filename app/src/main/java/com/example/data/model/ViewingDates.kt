package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Default viewing dates for listings that don't set their own: the next [count]
 * days starting tomorrow, labelled like "Tue, Oct 13" from the phone's clock.
 */
fun upcomingViewingDates(count: Int = 4): List<String> {
  val format = SimpleDateFormat("EEE, MMM d", Locale.ENGLISH)
  val day = Calendar.getInstance()
  return List(count) {
    day.add(Calendar.DAY_OF_YEAR, 1)
    format.format(day.time)
  }
}
