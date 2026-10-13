package com.example.data.model

import java.util.Locale

/**
 * Limits for listing values. The database enforces the same caps
 * (db-updates/5-listing-limits.sql), so keep the two in sync.
 */
object ListingLimits {
  // Top of the buyer filter sliders. A slider at its top means "no upper limit".
  const val FILTER_MAX_PRICE = 50_000_000L
  const val FILTER_MAX_SQFT = 10_000

  // Highest values an agent can enter on a listing.
  const val MAX_PRICE = 1_000_000_000L
  const val MAX_SQFT = 100_000
  const val MAX_ROOMS = 50.0

  /** Returns a message for the first value outside the allowed range, or null when all are valid. */
  fun validate(price: Long, beds: Double, baths: Double, sqft: Int): String? = when {
    price < 1 -> "Price must be at least ₱1."
    price > MAX_PRICE -> "Price can't be more than ₱${formatNumber(MAX_PRICE)}."
    beds < 0 || beds > MAX_ROOMS -> "Beds must be between 0 and ${MAX_ROOMS.toInt()}."
    baths < 0 || baths > MAX_ROOMS -> "Baths must be between 0 and ${MAX_ROOMS.toInt()}."
    sqft < 1 -> "Size must be at least 1 sqft."
    sqft > MAX_SQFT -> "Size can't be more than ${formatNumber(MAX_SQFT.toLong())} sqft."
    else -> null
  }

  /** Reads a typed number, ignoring commas and spaces. Too-large input becomes Long.MAX_VALUE so validate() rejects it. */
  fun parseWholeNumber(text: String): Long? {
    val digits = text.filter { it.isDigit() }
    if (digits.isEmpty()) return null
    return digits.toLongOrNull() ?: Long.MAX_VALUE
  }

  fun formatNumber(value: Long): String = String.format(Locale.getDefault(), "%,d", value)
}
