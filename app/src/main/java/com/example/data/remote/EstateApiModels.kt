package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackendStatusResponse(
  val success: Boolean = false,
  val status: String = "OFFLINE",
  val backend: String? = null,
  val database: String? = null,
  val postgres_version: String? = null,
  val server_time: String? = null,
  val table_stats: Map<String, Any?>? = null,
  val error: String? = null,
  val troubleshooting: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiResponse(
  val success: Boolean = false,
  val message: String? = null,
  val error: String? = null,
  val id: String? = null,
  val isFavorite: Boolean? = null,
  val token: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthUserDto(
  val id: String = "",
  val name: String = "",
  val email: String = "",
  val phone: String = "",
  val role: String = "BUYER",
  val licenseNo: String = "",
  val agency: String = "",
  val professionalRole: String = "Licensed Agent"
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
  val success: Boolean = false,
  val message: String? = null,
  val error: String? = null,
  val user: AuthUserDto? = null,
  val token: String? = null
)

@JsonClass(generateAdapter = true)
data class PropertyDto(
  val id: String,
  val title: String,
  val address: String,
  val cityStateZip: String = "",
  val price: Long = 0L,
  val priceFormatted: String = "",
  val isRental: Boolean = false,
  val beds: Double = 1.0,
  val baths: Double = 1.0,
  val sqft: Int = 500,
  val propertyType: String = "Condominium",
  val description: String = "",
  val imageResId: Int = 0,
  val mediaUris: List<String> = emptyList(),
  val isFavorite: Boolean = false,
  val status: String = "Active",
  val availableDates: List<String> = emptyList(),
  val availableTimeSlots: List<String> = emptyList(),
  val amenities: List<String> = emptyList(),
  val yearBuilt: Int = 2022,
  val agentName: String = "Sarah Jenkins",
  val agentTitle: String = "Premier Real Estate Broker",
  val agentPhone: String = "+63 917 555 0192",
  val agentEmail: String = "sarah.jenkins@estateflow.com"
)

@JsonClass(generateAdapter = true)
data class ViewingAppointmentDto(
  val id: String,
  val propertyId: String,
  val propertyTitle: String,
  val propertyAddress: String,
  val clientName: String,
  val clientPhone: String = "",
  val clientEmail: String = "",
  val date: String,
  val timeSlot: String,
  val type: String = "Viewing",
  val status: String = "UPCOMING",
  val notes: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
  val id: String,
  val sender: String,
  val text: String,
  val time: String = "Just now",
  val isFromMe: Boolean = false
)

@JsonClass(generateAdapter = true)
data class InquiryDto(
  val id: String,
  val propertyId: String,
  val propertyTitle: String,
  val propertyAddress: String = "",
  val senderName: String,
  val senderEmail: String,
  val senderPhone: String = "",
  val message: String,
  val timeAgo: String = "Just now",
  val isUnread: Boolean = true,
  val messages: List<ChatMessageDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
  val id: String,
  val title: String,
  val message: String,
  val timestampFormatted: String = "Just now",
  val type: String = "VIEWING_BOOKED",
  val isRead: Boolean = false,
  val targetId: String? = null
)

@JsonClass(generateAdapter = true)
data class AgentProfileDto(
  val name: String,
  val title: String,
  val licenseNo: String,
  val phone: String,
  val email: String,
  val rating: Double = 4.9,
  val reviewsCount: Int = 120,
  val totalSalesVolume: String = "₱48.2M",
  val activeListingsCount: Int = 0,
  val bio: String = ""
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
  val success: Boolean = false,
  val timestamp: Long = 0L,
  val database: String? = null,
  val properties: List<PropertyDto>? = null,
  val appointments: List<ViewingAppointmentDto>? = null,
  val inquiries: List<InquiryDto>? = null,
  val notifications: List<NotificationDto>? = null,
  val agentProfile: AgentProfileDto? = null
)

typealias SyncResponseDto = SyncResponse

@JsonClass(generateAdapter = true)
data class SyncRequestDto(
  val properties: List<PropertyDto> = emptyList(),
  val appointments: List<ViewingAppointmentDto> = emptyList()
)
