package com.example.data.model

/**
 * PRC real estate broker license number, e.g. PRC-REB-0094821.
 * The backend (auth.php) and the database (db-updates/9-license-format.sql)
 * enforce the same format, so keep the three in sync.
 */
object LicenseNumber {
  const val EXAMPLE = "PRC-REB-0012345"
  const val LENGTH = 15
  private val FORMAT = Regex("^PRC-REB-[0-9]{7}$")

  /** Upper-cases and strips spaces so "prc-reb-0012345 " is accepted as typed. */
  fun normalize(input: String): String = input.uppercase().filterNot { it.isWhitespace() }

  fun isValid(input: String): Boolean = FORMAT.matches(normalize(input))

  const val ERROR = "License number must look like $EXAMPLE: PRC-REB- followed by exactly 7 digits."
}
