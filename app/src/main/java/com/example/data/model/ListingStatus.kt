package com.example.data.model

/**
 * Listing statuses and what each one allows. The database accepts exactly
 * these values (properties_status_valid), so keep the two in sync.
 */
object ListingStatus {
  const val ACTIVE = "Active"
  const val NEW_LISTING = "New Listing"
  const val UNDER_OFFER = "Under Offer"
  const val PENDING = "Pending"
  const val SOLD = "Sold"

  /** Choices in the Edit Listing form, in the order a sale moves through them. */
  val ALL = listOf(NEW_LISTING, ACTIVE, UNDER_OFFER, PENDING, SOLD)

  fun isSold(status: String) = status.equals(SOLD, ignoreCase = true)

  /** Pending and Sold listings no longer take viewings or inquiries. */
  fun acceptsBuyers(status: String) =
    !status.equals(PENDING, ignoreCase = true) && !isSold(status)

  /** Short note for buyers on the listing page, or null when there is nothing to say. */
  fun buyerNote(status: String): String? = when {
    isSold(status) -> "This property has been sold and is no longer available."
    status.equals(PENDING, ignoreCase = true) -> "A sale is being finalised, so this property is no longer taking viewings."
    status.equals(UNDER_OFFER, ignoreCase = true) -> "An offer is being negotiated. You can still book a viewing or ask a question."
    else -> null
  }
}
