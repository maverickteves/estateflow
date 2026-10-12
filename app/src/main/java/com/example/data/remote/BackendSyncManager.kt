package com.example.data.remote

import com.example.data.model.upcomingViewingDates
import android.content.Context
import com.example.data.local.EstateDatabase
import com.example.data.local.entity.InquiryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PropertyEntity
import com.example.data.local.entity.ViewingAppointmentEntity
import com.example.data.model.AppointmentStatus
import com.example.data.model.ChatMessage
import com.example.data.model.Inquiry
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationType
import com.example.data.model.Property
import com.example.data.model.UserAccount
import com.example.data.model.ViewingAppointment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

enum class BackendConnectionState {
  OFFLINE,
  CONNECTING,
  CONNECTED,
  ERROR
}

data class BackendConnectionInfo(
  val state: BackendConnectionState = BackendConnectionState.OFFLINE,
  val backendUrl: String = EstateApiClient.DEFAULT_EMULATOR_URL,
  val serverType: String = "PHP (XAMPP / Apache)",
  val databaseEngine: String = "PostgreSQL",
  val postgresVersion: String? = null,
  val lastPingTime: String? = null,
  val lastSyncTime: String? = null,
  val errorMessage: String? = null,
  val propertiesCount: Int = 0,
  val appointmentsCount: Int = 0,
  val inquiriesCount: Int = 0
)

class BackendSyncManager(
  private val apiClient: EstateApiClient,
  private val database: EstateDatabase
) {

  private val _connectionInfo = MutableStateFlow(
    BackendConnectionInfo(
      backendUrl = apiClient.baseUrl
    )
  )
  val connectionInfo: StateFlow<BackendConnectionInfo> = _connectionInfo.asStateFlow()

  /**
   * Pings status.php. Background checks pass [showConnecting] = false so the
   * status dot keeps its last known state instead of flickering on every check.
   */
  suspend fun testConnection(showConnecting: Boolean = true): Result<BackendStatusResponse> = withContext(Dispatchers.IO) {
    if (showConnecting) {
      _connectionInfo.value = _connectionInfo.value.copy(
        state = BackendConnectionState.CONNECTING,
        errorMessage = null
      )
    }

    try {
      val response = apiClient.service.getStatus()
      if (response.isSuccessful && response.body() != null) {
        val body = response.body()!!
        val stats = body.table_stats
        val propCount = (stats?.get("properties") as? Number)?.toInt() ?: 0
        val apptCount = (stats?.get("viewing_appointments") as? Number)?.toInt() ?: 0
        val inqCount = (stats?.get("inquiries") as? Number)?.toInt() ?: 0

        _connectionInfo.value = _connectionInfo.value.copy(
          state = BackendConnectionState.CONNECTED,
          serverType = body.backend ?: "PHP (XAMPP / Apache)",
          databaseEngine = body.database ?: "PostgreSQL",
          postgresVersion = body.postgres_version,
          lastPingTime = body.server_time ?: "Just now",
          propertiesCount = propCount,
          appointmentsCount = apptCount,
          inquiriesCount = inqCount,
          errorMessage = null
        )
        Result.success(body)
      } else {
        val errorMsg = "HTTP ${response.code()}: ${response.message()}"
        _connectionInfo.value = _connectionInfo.value.copy(
          state = BackendConnectionState.ERROR,
          errorMessage = errorMsg
        )
        Result.failure(Exception(errorMsg))
      }
    } catch (e: Exception) {
      val err = e.localizedMessage ?: "Failed to connect to PHP server at ${apiClient.baseUrl}"
      _connectionInfo.value = _connectionInfo.value.copy(
        state = BackendConnectionState.ERROR,
        errorMessage = err
      )
      Result.failure(e)
    }
  }

  suspend fun syncWithBackend(): Result<String> = withContext(Dispatchers.IO) {
    try {
      // 1. First test/refresh connection
      val testRes = testConnection()
      if (testRes.isFailure) {
        return@withContext Result.failure(testRes.exceptionOrNull() ?: Exception("Backend unreachable"))
      }

      // 2. Fetch full sync payload from PHP Postgres backend using sync()
      val syncData: SyncResponse = apiClient.service.sync()
      var updatedCount = 0

      // Sync properties with PostgreSQL as sole source of truth
      syncData.properties?.let { remoteProps ->
        val remoteIds = remoteProps.map { it.id }
        if (remoteIds.isNotEmpty()) {
          database.propertyDao().deletePropertiesNotIn(remoteIds)
        } else {
          database.propertyDao().clearAll()
        }

        for (dto in remoteProps) {
          val prop = Property(
            id = dto.id,
            title = dto.title,
            address = dto.address,
            cityStateZip = dto.cityStateZip,
            price = dto.price,
            priceFormatted = dto.priceFormatted,
            isRental = dto.isRental,
            beds = dto.beds,
            baths = dto.baths,
            sqft = dto.sqft,
            propertyType = dto.propertyType,
            description = dto.description,
            imageResId = dto.imageResId,
            mediaUris = dto.mediaUris,
            isFavorite = dto.isFavorite,
            status = dto.status,
            availableDates = dto.availableDates.ifEmpty { upcomingViewingDates() },
            availableTimeSlots = dto.availableTimeSlots.ifEmpty { listOf("10:00 AM", "1:30 PM", "4:00 PM", "5:30 PM") },
            amenities = dto.amenities.ifEmpty { listOf("Modern Design", "High Speed Internet") },
            yearBuilt = dto.yearBuilt,
            agentName = dto.agentName,
            agentTitle = dto.agentTitle,
            agentPhone = dto.agentPhone,
            agentEmail = dto.agentEmail
          )
          database.propertyDao().insertProperty(PropertyEntity.fromDomain(prop))
          updatedCount++
        }
      }

      // Sync appointments with PostgreSQL as sole source of truth
      syncData.appointments?.let { remoteAppts ->
        val remoteApptIds = remoteAppts.map { it.id }
        if (remoteApptIds.isNotEmpty()) {
          database.appointmentDao().deleteAppointmentsNotIn(remoteApptIds)
        } else {
          database.appointmentDao().clearAll()
        }

        for (dto in remoteAppts) {
          val status = try {
            AppointmentStatus.valueOf(dto.status.uppercase())
          } catch (e: Exception) {
            AppointmentStatus.UPCOMING
          }
          val appt = ViewingAppointment(
            id = dto.id,
            propertyId = dto.propertyId,
            propertyTitle = dto.propertyTitle,
            propertyAddress = dto.propertyAddress,
            clientName = dto.clientName,
            clientPhone = dto.clientPhone,
            clientEmail = dto.clientEmail,
            date = dto.date,
            timeSlot = dto.timeSlot,
            type = dto.type,
            status = status,
            notes = dto.notes,
            timestamp = dto.timestamp
          )
          database.appointmentDao().insertAppointment(ViewingAppointmentEntity.fromDomain(appt))
        }
      }

      // Sync inquiries with PostgreSQL as sole source of truth
      syncData.inquiries?.let { remoteInqs ->
        val remoteInqIds = remoteInqs.map { it.id }
        if (remoteInqIds.isNotEmpty()) {
          database.inquiryDao().deleteInquiriesNotIn(remoteInqIds)
        } else {
          database.inquiryDao().clearAll()
        }

        for (dto in remoteInqs) {
          val inq = Inquiry(
            id = dto.id,
            propertyId = dto.propertyId,
            propertyTitle = dto.propertyTitle,
            propertyAddress = dto.propertyAddress,
            senderName = dto.senderName,
            senderEmail = dto.senderEmail,
            senderPhone = dto.senderPhone,
            message = dto.message,
            timeAgo = dto.timeAgo,
            isUnread = dto.isUnread,
            messages = dto.messages.map { m ->
              ChatMessage(
                id = m.id,
                sender = m.sender,
                text = m.text,
                time = m.time,
                isFromMe = m.isFromMe
              )
            }
          )
          database.inquiryDao().insertInquiry(InquiryEntity.fromDomain(inq))
        }
      }

      syncData.notifications?.let { remoteNotifications ->
        database.notificationDao().clearAll()
        database.notificationDao().insertNotifications(
          remoteNotifications.map { dto ->
            NotificationEntity(
              id = dto.id,
              title = dto.title,
              message = dto.message,
              timestampFormatted = dto.timestampFormatted,
              type = try {
                NotificationType.valueOf(dto.type.uppercase())
              } catch (_: Exception) {
                NotificationType.LISTING_UPDATE
              },
              isRead = dto.isRead,
              targetId = dto.targetId,
              timestamp = System.currentTimeMillis()
            )
          }
        )
      }

      syncData.agentProfile?.let { dto ->
        database.agentProfileDao().insertOrUpdateProfile(
          com.example.data.local.entity.AgentProfileEntity(
            name = dto.name,
            title = dto.title,
            licenseNo = dto.licenseNo,
            phone = dto.phone,
            email = dto.email,
            rating = dto.rating,
            reviewsCount = dto.reviewsCount,
            totalSalesVolume = dto.totalSalesVolume,
            activeListingsCount = dto.activeListingsCount,
            bio = dto.bio
          )
        )
      }

      _connectionInfo.value = _connectionInfo.value.copy(
        state = BackendConnectionState.CONNECTED,
        lastSyncTime = "Just now",
        errorMessage = null
      )

      Result.success("Synced $updatedCount listings and schedules")
    } catch (e: Exception) {
      _connectionInfo.value = _connectionInfo.value.copy(
        errorMessage = "Sync error: ${e.localizedMessage}"
      )
      Result.failure(e)
    }
  }

  // Push individual property to PHP backend in background
  suspend fun pushProperty(property: Property) = withContext(Dispatchers.IO) {
    try {
      val dto = PropertyDto(
        id = property.id,
        title = property.title,
        address = property.address,
        cityStateZip = property.cityStateZip,
        price = property.price,
        priceFormatted = property.priceFormatted,
        isRental = property.isRental,
        beds = property.beds,
        baths = property.baths,
        sqft = property.sqft,
        propertyType = property.propertyType,
        description = property.description,
        imageResId = property.imageResId,
        mediaUris = property.mediaUris,
        isFavorite = property.isFavorite,
        status = property.status,
        availableDates = property.availableDates,
        availableTimeSlots = property.availableTimeSlots,
        amenities = property.amenities,
        yearBuilt = property.yearBuilt,
        agentName = property.agentName,
        agentTitle = property.agentTitle,
        agentPhone = property.agentPhone,
        agentEmail = property.agentEmail
      )
      apiClient.service.createProperty(dto)
    } catch (_: Exception) {}
  }

  // Push individual appointment to PHP backend in background
  suspend fun pushAppointment(appt: ViewingAppointment) = withContext(Dispatchers.IO) {
    try {
      val dto = ViewingAppointmentDto(
        id = appt.id,
        propertyId = appt.propertyId,
        propertyTitle = appt.propertyTitle,
        propertyAddress = appt.propertyAddress,
        clientName = appt.clientName,
        clientPhone = appt.clientPhone,
        clientEmail = appt.clientEmail,
        date = appt.date,
        timeSlot = appt.timeSlot,
        type = appt.type,
        status = appt.status.name,
        notes = appt.notes,
        timestamp = appt.timestamp
      )
      apiClient.service.bookAppointment(dto)
    } catch (_: Exception) {}
  }

  // Call POST /properties.php (Create or Update property in PostgreSQL)
  suspend fun createOrUpdatePropertyOnBackend(property: Property): Result<PropertyDto> = withContext(Dispatchers.IO) {
    try {
      val dto = PropertyDto(
        id = property.id,
        title = property.title,
        address = property.address,
        cityStateZip = property.cityStateZip,
        price = property.price,
        priceFormatted = property.priceFormatted,
        isRental = property.isRental,
        beds = property.beds,
        baths = property.baths,
        sqft = property.sqft,
        propertyType = property.propertyType,
        description = property.description,
        imageResId = property.imageResId,
        mediaUris = property.mediaUris,
        isFavorite = property.isFavorite,
        status = property.status,
        availableDates = property.availableDates,
        availableTimeSlots = property.availableTimeSlots,
        amenities = property.amenities,
        yearBuilt = property.yearBuilt,
        agentName = property.agentName,
        agentTitle = property.agentTitle,
        agentPhone = property.agentPhone,
        agentEmail = property.agentEmail
      )
      val response = apiClient.service.createProperty(dto)
      if (response.isSuccessful && response.body() != null) {
        syncWithBackend()
        Result.success(response.body()!!)
      } else {
        Result.failure(Exception("PostgreSQL create failed with HTTP ${response.code()}: ${response.message()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun toggleFavoriteOnBackend(propertyId: String): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.toggleFavorite(propertyId)
      if (response.isSuccessful && response.body()?.success == true) {
        val fav = response.body()?.isFavorite ?: false
        syncWithBackend()
        Result.success(fav)
      } else {
        Result.failure(Exception(response.body()?.error ?: "Failed to update favorite in PostgreSQL"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateAppointmentStatusOnBackend(id: String, status: String): Result<ApiResponse> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.updateAppointmentStatus(id, status)
      if (response.isSuccessful && response.body() != null) {
        syncWithBackend()
        Result.success(response.body()!!)
      } else {
        Result.failure(Exception("HTTP ${response.code()}"))
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushInquiry(inquiry: Inquiry): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val dto = InquiryDto(
        id = inquiry.id,
        propertyId = inquiry.propertyId,
        propertyTitle = inquiry.propertyTitle,
        propertyAddress = inquiry.propertyAddress,
        senderName = inquiry.senderName,
        senderEmail = inquiry.senderEmail,
        senderPhone = inquiry.senderPhone,
        message = inquiry.message,
        timeAgo = inquiry.timeAgo,
        isUnread = inquiry.isUnread,
        messages = inquiry.messages.map { message ->
          ChatMessageDto(
            id = message.id,
            sender = message.sender,
            text = message.text,
            time = message.time,
            isFromMe = message.isFromMe
          )
        }
      )
      val response = apiClient.service.sendInquiry(dto)
      if (!response.isSuccessful || response.body() == null) {
        return@withContext Result.failure(Exception("PostgreSQL inquiry save failed: HTTP ${response.code()}"))
      }
      syncWithBackend()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun replyToInquiryOnBackend(inquiryId: String, text: String, isAgent: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.replyInquiry(
        mapOf(
          "inquiryId" to inquiryId,
          "replyText" to text,
          "isAgent" to isAgent.toString()
        )
      )
      if (!response.isSuccessful || response.body()?.success != true) {
        return@withContext Result.failure(Exception(response.body()?.error ?: "PostgreSQL reply save failed: HTTP ${response.code()}"))
      }
      syncWithBackend()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun markInquiryReadOnBackend(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.markInquiryRead(id)
      if (!response.isSuccessful || response.body()?.success != true) {
        return@withContext Result.failure(Exception(response.body()?.error ?: "Failed to mark inquiry as read"))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun markNotificationReadOnBackend(id: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.markNotificationRead(id)
      if (!response.isSuccessful || response.body()?.success != true) {
        return@withContext Result.failure(Exception(response.body()?.error ?: "Failed to mark notification as read"))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun markAllNotificationsReadOnBackend(): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.markAllNotificationsRead()
      if (!response.isSuccessful || response.body()?.success != true) {
        return@withContext Result.failure(Exception(response.body()?.error ?: "Failed to mark notifications as read"))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun pushNotification(notification: NotificationItem): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.createNotification(
        NotificationDto(
          id = notification.id,
          title = notification.title,
          message = notification.message,
          timestampFormatted = notification.timestampFormatted,
          type = notification.type.name,
          isRead = notification.isRead,
          targetId = notification.targetId
        )
      )
      if (!response.isSuccessful || response.body() == null) {
        return@withContext Result.failure(Exception("PostgreSQL notification save failed: HTTP ${response.code()}"))
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /** Network failures shown to users must not reveal server addresses. */
  private fun userFacingNetworkError(e: Exception): String = when (e) {
    is java.io.IOException -> "Can't reach Estateflow right now. Check your connection and try again."
    else -> "Something went wrong. Please try again."
  }

  private fun parseAuthError(response: retrofit2.Response<*>): String {
    val rawError = response.errorBody()?.string()?.trim() ?: ""
    if (rawError.isNotBlank()) {
      try {
        val parsed = apiClient.moshi.adapter(AuthResponse::class.java).fromJson(rawError)
        if (!parsed?.error.isNullOrBlank()) return parsed!!.error!!
        if (!parsed?.message.isNullOrBlank()) return parsed!!.message!!
      } catch (_: Exception) {}
      if (!rawError.startsWith("<")) return rawError
    }
    return "Something went wrong (error ${response.code()}). Please try again."
  }

  suspend fun loginClientOnBackend(email: String, password: String = ""): Result<UserAccount> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.clientLogin(mapOf("email" to email, "password" to password))
      val body = response.body()
      if (response.isSuccessful && body?.success == true && body.user != null) {
        return@withContext Result.success(body.user.toDomain())
      }
      val errorMsg = body?.error ?: parseAuthError(response)
      Result.failure(Exception(errorMsg))
    } catch (e: Exception) {
      Result.failure(Exception(userFacingNetworkError(e)))
    }
  }

  suspend fun signupClientOnBackend(name: String, email: String, phone: String, password: String = ""): Result<UserAccount> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.clientSignup(
        mapOf(
          "name" to name,
          "email" to email,
          "phone" to phone,
          "password" to password
        )
      )
      val body = response.body()
      if (response.isSuccessful && body?.success == true && body.user != null) {
        return@withContext Result.success(body.user.toDomain())
      }
      val errorMsg = body?.error ?: parseAuthError(response)
      Result.failure(Exception(errorMsg))
    } catch (e: Exception) {
      Result.failure(Exception(userFacingNetworkError(e)))
    }
  }

  suspend fun loginAgentOnBackend(identifier: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.agentLogin(mapOf("emailOrLicense" to identifier, "password" to password))
      val body = response.body()
      if (response.isSuccessful && body?.success == true && body.user != null) {
        return@withContext Result.success(body.user.toDomain())
      }
      val errorMsg = body?.error ?: parseAuthError(response)
      Result.failure(Exception(errorMsg))
    } catch (e: Exception) {
      Result.failure(Exception(userFacingNetworkError(e)))
    }
  }

  suspend fun signupAgentOnBackend(name: String, email: String, licenseNo: String, agency: String, professionalRole: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.agentSignup(
        mapOf(
          "name" to name,
          "email" to email,
          "licenseNo" to licenseNo,
          "agency" to agency,
          "professionalRole" to professionalRole,
          "password" to password
        )
      )
      val body = response.body()
      if (response.isSuccessful && body?.success == true && body.user != null) {
        return@withContext Result.success(body.user.toDomain())
      }
      val errorMsg = body?.error ?: parseAuthError(response)
      Result.failure(Exception(errorMsg))
    } catch (e: Exception) {
      Result.failure(Exception(userFacingNetworkError(e)))
    }
  }

  suspend fun updatePasswordOnBackend(email: String, currentPassword: String, newPassword: String): Result<ApiResponse> = withContext(Dispatchers.IO) {
    try {
      val response = apiClient.service.updatePassword(
        mapOf("email" to email, "currentPassword" to currentPassword, "newPassword" to newPassword)
      )
      if (!response.isSuccessful || response.body()?.success != true) {
        return@withContext Result.failure(Exception(response.body()?.error ?: "Password update failed. Please try again."))
      }
      Result.success(response.body()!!)
    } catch (e: Exception) {
      Result.failure(Exception(userFacingNetworkError(e)))
    }
  }

  private fun AuthUserDto.toDomain(): UserAccount = UserAccount(
    id = id,
    name = name,
    email = email,
    phone = phone,
    role = if (role.equals("AGENT", ignoreCase = true)) com.example.data.model.UserRole.AGENT else com.example.data.model.UserRole.BUYER,
    licenseNo = licenseNo,
    agency = agency,
    professionalRole = professionalRole
  )

  // Call DELETE /estateflow-backend/api/properties.php?id={PROPERTY_ID}
  suspend fun deleteProperty(propertyId: String): Result<ApiResponse> = withContext(Dispatchers.IO) {
    try {
      android.util.Log.d("BackendSyncManager", "Sending DELETE for property $propertyId to ${apiClient.baseUrl}")
      val response = apiClient.service.deleteProperty(propertyId)
      if (response.isSuccessful && response.body() != null) {
        val body = response.body()!!
        if (body.success) {
          android.util.Log.d("BackendSyncManager", "Postgres delete success: ${body.message}")
          syncWithBackend()
          Result.success(body)
        } else {
          val err = body.error ?: body.message ?: "PostgreSQL returned failure"
          android.util.Log.e("BackendSyncManager", "Postgres delete reported error: $err")
          Result.failure(Exception(err))
        }
      } else {
        val rawError = response.errorBody()?.string()?.trim() ?: ""
        val errorMsg = when {
          rawError.contains("<!doctype html", ignoreCase = true) || rawError.contains("<html", ignoreCase = true) -> {
            if (response.code() == 404) {
              "HTTP 404: Endpoint not found at ${apiClient.baseUrl}properties.php. Verify XAMPP htdocs/estateflow-backend folder."
            } else {
              "Server returned HTML (HTTP ${response.code()}). Check that Apache and PostgreSQL are running in XAMPP."
            }
          }
          rawError.isNotBlank() -> {
            try {
              val parsed = apiClient.moshi.adapter(ApiResponse::class.java).fromJson(rawError)
              parsed?.error ?: parsed?.message ?: rawError
            } catch (_: Exception) {
              rawError
            }
          }
          else -> "HTTP ${response.code()}: ${response.message()}"
        }
        android.util.Log.e("BackendSyncManager", "Delete failed: $errorMsg")
        Result.failure(Exception(errorMsg))
      }
    } catch (e: Exception) {
      val msg = when {
        e.message?.contains("malformed JSON", ignoreCase = true) == true ||
        e.message?.contains("JsonReader.setLenient", ignoreCase = true) == true ->
          "Server returned HTML (welcome/error page). Verify your Backend URL in Settings points to /estateflow-backend/api/."
        else -> e.localizedMessage ?: "Network error connecting to ${apiClient.baseUrl}"
      }
      android.util.Log.e("BackendSyncManager", "Exception calling deleteProperty: $msg", e)
      Result.failure(Exception(msg, e))
    }
  }

  companion object {
    @Volatile
    private var INSTANCE: BackendSyncManager? = null

    fun getInstance(context: Context): BackendSyncManager {
      return INSTANCE ?: synchronized(this) {
        val database = EstateDatabase.getDatabase(context)
        val apiClient = EstateApiClient.getInstance(context)
        val instance = BackendSyncManager(apiClient, database)
        INSTANCE = instance
        instance
      }
    }
  }
}
