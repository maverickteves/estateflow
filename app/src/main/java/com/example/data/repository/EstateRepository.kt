package com.example.data.repository

import com.example.data.model.upcomingViewingDates
import android.content.Context
import com.example.R
import com.example.data.local.EstateDatabase
import com.example.data.local.entity.AgentProfileEntity
import com.example.data.local.entity.InquiryEntity
import com.example.data.local.entity.NotificationEntity
import com.example.data.local.entity.PropertyEntity
import com.example.data.local.entity.UserSessionEntity
import com.example.data.local.entity.ViewingAppointmentEntity
import com.example.data.model.AgentProfile
import com.example.data.model.LicenseNumber
import com.example.data.model.AppointmentStatus
import com.example.data.model.ChatMessage
import com.example.data.model.Inquiry
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationType
import com.example.data.model.Property
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.data.model.ViewingAppointment
import com.example.data.remote.BackendConnectionInfo
import com.example.data.remote.BackendSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class EstateRepository private constructor(
  private val database: EstateDatabase,
  val syncManager: BackendSyncManager? = null,
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

  val backendConnectionInfo: StateFlow<BackendConnectionInfo>? = syncManager?.connectionInfo

  val activeSession: Flow<UserAccount?> = database.userSessionDao().getActiveSession().map { it?.toDomain() }

  val properties: Flow<List<Property>> = database.propertyDao().getAllProperties().map { list ->
    list.map { it.toDomain() }
  }

  val appointments: Flow<List<ViewingAppointment>> = database.appointmentDao().getAllAppointments().map { list ->
    list.map { it.toDomain() }
  }

  val inquiries: Flow<List<Inquiry>> = database.inquiryDao().getAllInquiries().map { list ->
    list.map { it.toDomain() }
  }

  val notifications: Flow<List<NotificationItem>> = database.notificationDao().getAllNotifications().map { list ->
    list.map { it.toDomain() }
  }

  val agentProfile: Flow<AgentProfile> = database.agentProfileDao().getProfile().map {
    it?.toDomain() ?: AgentProfile()
  }

  init {
    coroutineScope.launch {
      // Clear legacy demo properties, appointments, inquiries, notifications if present
      val legacy = database.propertyDao().findPropertyById("prop_townhouse")
      if (legacy != null) {
        database.propertyDao().clearAll()
        database.appointmentDao().clearAll()
        database.inquiryDao().clearAll()
        database.notificationDao().clearAll()
      }
      // Ensure the demo agent account profile is present
      if (database.agentProfileDao().getCount() == 0) {
        database.agentProfileDao().insertOrUpdateProfile(AgentProfileEntity.fromDomain(AgentProfile()))
      }
      // Auto-sync immediately from PostgreSQL backend
      syncManager?.syncWithBackend()
    }
  }

  fun toggleFavorite(propertyId: String) {
    coroutineScope.launch {
      val current = database.propertyDao().findPropertyById(propertyId)
      if (current != null) {
        database.propertyDao().updateFavorite(propertyId, !current.isFavorite)
      }
      syncManager?.toggleFavoriteOnBackend(propertyId)
    }
  }

  suspend fun bookAppointment(
    propertyId: String,
    date: String,
    timeSlot: String,
    clientName: String,
    clientPhone: String,
    clientEmail: String,
    notes: String
  ): ViewingAppointment = withContext(Dispatchers.IO) {
    val property = database.propertyDao().findPropertyById(propertyId)
    val newAppointment = ViewingAppointment(
      id = UUID.randomUUID().toString(),
      propertyId = propertyId,
      propertyTitle = property?.title ?: "Property Viewing",
      propertyAddress = property?.address ?: "Address",
      clientName = clientName.ifBlank { "You (Verified Buyer)" },
      clientPhone = clientPhone.ifBlank { "+63 917 555 0134" },
      clientEmail = clientEmail.ifBlank { "buyer@estateflow.com" },
      date = date,
      timeSlot = timeSlot,
      type = "Viewing",
      status = AppointmentStatus.UPCOMING,
      notes = notes.ifBlank { "Scheduled via Estateflow instant booking." },
      timestamp = System.currentTimeMillis()
    )

    database.appointmentDao().insertAppointment(ViewingAppointmentEntity.fromDomain(newAppointment))

    val newNotification = NotificationItem(
      id = UUID.randomUUID().toString(),
      title = "Viewing Appointment Booked",
      message = "Viewing for ${newAppointment.propertyTitle} confirmed on $date at $timeSlot.",
      timestampFormatted = "Just now",
      type = NotificationType.VIEWING_BOOKED,
      isRead = false,
      targetId = newAppointment.id
    )
    database.notificationDao().insertNotification(NotificationEntity.fromDomain(newNotification))

    coroutineScope.launch {
      syncManager?.pushAppointment(newAppointment)
    }

    newAppointment
  }

  fun updateAppointmentStatus(appointmentId: String, newStatus: AppointmentStatus) {
    coroutineScope.launch {
      database.appointmentDao().updateStatus(appointmentId, newStatus)
      syncManager?.updateAppointmentStatusOnBackend(appointmentId, newStatus.name)
    }
  }

  suspend fun sendInquiry(
    propertyId: String,
    messageText: String,
    senderName: String = "You (Prospective Buyer)",
    senderEmail: String = "buyer@estateflow.com",
    senderPhone: String = "+63 917 555 0134"
  ): Inquiry = withContext(Dispatchers.IO) {
    val property = database.propertyDao().findPropertyById(propertyId)
    val existingInquiry = database.inquiryDao().findInquiryByPropertyAndSender(propertyId, senderEmail)

    if (existingInquiry != null) {
      val updatedMessages = existingInquiry.messages + ChatMessage(
        id = UUID.randomUUID().toString(),
        sender = "user",
        text = messageText,
        time = "Just now",
        isFromMe = true
      )
      val updatedInquiry = existingInquiry.copy(
        message = messageText,
        timeAgo = "Just now",
        isUnread = true,
        messages = updatedMessages,
        updatedAt = System.currentTimeMillis()
      )
      database.inquiryDao().insertInquiry(updatedInquiry)
      syncManager?.pushInquiry(updatedInquiry.toDomain())
      updatedInquiry.toDomain()
    } else {
      val newInquiry = Inquiry(
        id = UUID.randomUUID().toString(),
        propertyId = propertyId,
        propertyTitle = property?.title ?: "Property Inquiry",
        propertyAddress = property?.address ?: "",
        senderName = senderName,
        senderEmail = senderEmail,
        senderPhone = senderPhone,
        message = messageText,
        timeAgo = "Just now",
        isUnread = true,
        messages = listOf(
          ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = "user",
            text = messageText,
            time = "Just now",
            isFromMe = true
          )
        )
      )
      database.inquiryDao().insertInquiry(InquiryEntity.fromDomain(newInquiry))

      val notif = NotificationItem(
        id = UUID.randomUUID().toString(),
        title = "Inquiry Sent to ${property?.agentName ?: "Agent"}",
        message = "Your inquiry regarding ${property?.title ?: "property"} has been delivered.",
        timestampFormatted = "Just now",
        type = NotificationType.NEW_INQUIRY,
        isRead = false,
        targetId = newInquiry.id
      )
      database.notificationDao().insertNotification(NotificationEntity.fromDomain(notif))

      syncManager?.pushInquiry(newInquiry)

      newInquiry
    }
  }

  suspend fun replyToInquiry(inquiryId: String, replyText: String, isAgent: Boolean = true) = withContext(Dispatchers.IO) {
    val existing = database.inquiryDao().getInquiryById(inquiryId) ?: return@withContext
    val newMsg = ChatMessage(
      id = UUID.randomUUID().toString(),
      sender = if (isAgent) "agent" else "user",
      text = replyText,
      time = "Just now",
      isFromMe = isAgent
    )
    val updated = existing.copy(
      isUnread = !isAgent,
      message = replyText,
      timeAgo = "Just now",
      messages = existing.messages + newMsg,
      updatedAt = System.currentTimeMillis()
    )
    database.inquiryDao().insertInquiry(updated)
    syncManager?.replyToInquiryOnBackend(inquiryId, replyText, isAgent)
  }

  fun markInquiryAsRead(id: String) {
    coroutineScope.launch {
      database.inquiryDao().markAsRead(id)
      syncManager?.markInquiryReadOnBackend(id)
    }
  }

  fun markNotificationAsRead(id: String) {
    coroutineScope.launch {
      database.notificationDao().markAsRead(id)
      syncManager?.markNotificationReadOnBackend(id)
    }
  }

  fun markAllNotificationsAsRead() {
    coroutineScope.launch {
      database.notificationDao().markAllAsRead()
      syncManager?.markAllNotificationsReadOnBackend()
    }
  }

  suspend fun addProperty(
    title: String,
    address: String,
    cityStateZip: String,
    price: Long,
    isRental: Boolean,
    beds: Double,
    baths: Double,
    sqft: Int,
    propertyType: String,
    description: String,
    amenities: List<String>,
    mediaUris: List<String> = emptyList(),
    availableDates: List<String> = upcomingViewingDates(),
    availableTimeSlots: List<String> = listOf("10:00 AM", "1:30 PM", "4:00 PM", "5:30 PM")
  ): Property = withContext(Dispatchers.IO) {
    val formattedPrice = if (isRental) "₱${String.format("%,d", price)} / mo" else "₱${String.format("%,d", price)}"
    // Stamp the listing with the signed-in agent, so buyers' chats and bookings reach that agent.
    val agent = database.userSessionDao().findActiveSession()?.toDomain()
    val listing = Property(
      id = UUID.randomUUID().toString(),
      title = title,
      address = address,
      cityStateZip = cityStateZip,
      price = price,
      priceFormatted = formattedPrice,
      isRental = isRental,
      beds = beds,
      baths = baths,
      sqft = sqft,
      propertyType = propertyType,
      description = description,
      imageResId = 0,
      mediaUris = mediaUris,
      isFavorite = false,
      status = "New Listing",
      availableDates = availableDates.ifEmpty { upcomingViewingDates() },
      availableTimeSlots = availableTimeSlots.ifEmpty { listOf("10:00 AM", "1:30 PM", "4:00 PM", "5:30 PM") },
      amenities = if (amenities.isEmpty()) listOf("Modern Design", "High Speed Internet", "Near Transit") else amenities
    )
    val newProperty = if (agent == null || agent.email.isBlank()) listing else listing.copy(
      agentName = agent.name.ifBlank { listing.agentName },
      agentTitle = "${agent.professionalRole} • ${agent.agency.ifBlank { "Estateflow Luxury Realty" }}",
      agentPhone = agent.phone.ifBlank { listing.agentPhone },
      agentEmail = agent.email
    )

    database.propertyDao().insertProperty(PropertyEntity.fromDomain(newProperty))

    val notif = NotificationItem(
      id = UUID.randomUUID().toString(),
      title = "New Property Published",
      message = "${newProperty.title} is now live on Estateflow with active viewing bookings.",
      timestampFormatted = "Just now",
      type = NotificationType.LISTING_UPDATE,
      isRead = false,
      targetId = newProperty.id
    )
    database.notificationDao().insertNotification(NotificationEntity.fromDomain(notif))

    try {
      syncManager?.createOrUpdatePropertyOnBackend(newProperty)
    } catch (_: Exception) {}

    newProperty
  }

  suspend fun updateProperty(property: Property): Property = withContext(Dispatchers.IO) {
    val formattedPrice = if (property.isRental) "₱${String.format("%,d", property.price)} / mo" else "₱${String.format("%,d", property.price)}"
    val updatedProperty = property.copy(priceFormatted = formattedPrice)
    database.propertyDao().insertProperty(PropertyEntity.fromDomain(updatedProperty))

    val notif = NotificationItem(
      id = UUID.randomUUID().toString(),
      title = "Listing Updated",
      message = "${updatedProperty.title} details and media have been updated.",
      timestampFormatted = "Just now",
      type = NotificationType.LISTING_UPDATE,
      isRead = false,
      targetId = updatedProperty.id
    )
    database.notificationDao().insertNotification(NotificationEntity.fromDomain(notif))

    try {
      syncManager?.createOrUpdatePropertyOnBackend(updatedProperty)
    } catch (_: Exception) {}

    updatedProperty
  }

  suspend fun deleteProperty(propertyId: String): PropertyDeleteResult = withContext(Dispatchers.IO) {
    val prop = database.propertyDao().findPropertyById(propertyId)
    val title = prop?.title ?: "Property"

    // 1. Call Backend API: DELETE /estateflow-backend/api/properties.php?id={PROPERTY_ID}
    var backendDeleted = false
    var backendError: String? = null

    if (syncManager != null) {
      val res = syncManager.deleteProperty(propertyId)
      if (res.isSuccess) {
        backendDeleted = true
      } else {
        backendError = res.exceptionOrNull()?.localizedMessage ?: "Unknown backend error"
      }
    }

    // 2. Remove property from local Room database
    database.propertyDao().deletePropertyById(propertyId)

    // 3. Handle any appointments related to the deleted property
    database.appointmentDao().cancelAppointmentsForProperty(
      propertyId = propertyId,
      notes = "Viewing cancelled: Listing \"$title\" was removed by agent."
    )

    // 4. Record listing update notification
    val notif = NotificationItem(
      id = UUID.randomUUID().toString(),
      title = "Listing Removed",
      message = if (backendDeleted) {
        "$title has been permanently removed from active listings."
      } else if (backendError != null) {
        "$title was removed from this device, but the change could not be saved online."
      } else {
        "$title has been removed from active listings, and related viewing appointments were cancelled."
      },
      timestampFormatted = "Just now",
      type = NotificationType.LISTING_UPDATE,
      isRead = false,
      targetId = propertyId
    )
    database.notificationDao().insertNotification(NotificationEntity.fromDomain(notif))

    PropertyDeleteResult(
      success = true,
      title = title,
      backendDeleted = backendDeleted,
      backendError = backendError
    )
  }

  suspend fun loginClient(email: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    val cleanEmail = email.trim()
    val cleanPassword = password.trim()
    if (cleanEmail.isBlank()) {
      return@withContext Result.failure(Exception("Please enter your email address."))
    }
    if (cleanPassword.isBlank()) {
      return@withContext Result.failure(Exception("Please enter your password."))
    }

    val remoteResult = syncManager?.loginClientOnBackend(cleanEmail, cleanPassword)
    if (remoteResult != null) {
      if (remoteResult.isSuccess) {
        val account = remoteResult.getOrThrow()
        database.userSessionDao().clearActiveSessions()
        database.userSessionDao().insertSession(UserSessionEntity.fromDomain(account))
        return@withContext Result.success(account)
      } else {
        return@withContext Result.failure(remoteResult.exceptionOrNull()!!)
      }
    }

    val session = database.userSessionDao().findActiveSession()
    if (session != null && session.email.equals(cleanEmail, ignoreCase = true) && session.role.equals("BUYER", ignoreCase = true)) {
      return@withContext Result.success(session.toDomain())
    }

    Result.failure(Exception("Account not found for email '$cleanEmail'. Please create an account first."))
  }

  suspend fun signUpClient(name: String, email: String, phone: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    val cleanName = name.trim()
    val cleanEmail = email.trim()
    val cleanPhone = phone.trim()
    val cleanPassword = password.trim()

    if (cleanName.isBlank() || cleanEmail.isBlank()) {
      return@withContext Result.failure(Exception("Full name and email address are required to create an account."))
    }
    if (cleanPassword.length < 8) {
      return@withContext Result.failure(Exception("Password must be at least 8 characters."))
    }

    val remoteResult = syncManager?.signupClientOnBackend(cleanName, cleanEmail, cleanPhone, cleanPassword)
    if (remoteResult != null) {
      if (remoteResult.isSuccess) {
        val account = remoteResult.getOrThrow()
        database.userSessionDao().clearActiveSessions()
        database.userSessionDao().insertSession(UserSessionEntity.fromDomain(account))
        return@withContext Result.success(account)
      } else {
        return@withContext Result.failure(remoteResult.exceptionOrNull()!!)
      }
    }

    val account = UserAccount(
      id = "client_${UUID.randomUUID().toString().take(8)}",
      name = cleanName,
      email = cleanEmail,
      phone = if (cleanPhone.isNotBlank()) cleanPhone else "+63 917 555 0134",
      role = UserRole.BUYER
    )
    database.userSessionDao().clearActiveSessions()
    database.userSessionDao().insertSession(UserSessionEntity.fromDomain(account))
    Result.success(account)
  }

  suspend fun loginAgent(emailOrLicense: String, password: String): Result<UserAccount> = withContext(Dispatchers.IO) {
    val input = emailOrLicense.trim()
    if (input.isBlank()) {
      return@withContext Result.failure(Exception("Please enter your agent email or PRC license number."))
    }
    if (password.length < 4) {
      return@withContext Result.failure(Exception("Password must be at least 4 characters."))
    }

    val remoteAccount = syncManager?.loginAgentOnBackend(input, password)
    if (remoteAccount != null) {
      if (remoteAccount.isFailure) return@withContext Result.failure(remoteAccount.exceptionOrNull()!!)
      val account = remoteAccount.getOrThrow()
      database.userSessionDao().clearActiveSessions()
      database.userSessionDao().insertSession(UserSessionEntity.fromDomain(account))
      return@withContext Result.success(account)
    }

    Result.failure(Exception("Agent account not found for '$input'. Please register as a licensed agent first."))
  }

  suspend fun signUpAgent(
    name: String,
    email: String,
    licenseNo: String,
    agency: String,
    professionalRole: String,
    password: String
  ): Result<UserAccount> = withContext(Dispatchers.IO) {
    val cleanName = name.trim()
    val cleanEmail = email.trim()
    val cleanLicense = LicenseNumber.normalize(licenseNo)
    val cleanAgency = agency.trim().ifBlank { "Estateflow Luxury Realty" }
    val cleanRole = professionalRole.trim().ifBlank { "Licensed Real Estate Agent" }

    if (cleanName.isBlank()) return@withContext Result.failure(Exception("Please enter your legal name."))
    if (cleanEmail.isBlank()) return@withContext Result.failure(Exception("Please enter your agent email."))
    if (cleanLicense.isBlank()) return@withContext Result.failure(Exception("Please enter your PRC real estate license number."))
    if (!LicenseNumber.isValid(cleanLicense)) return@withContext Result.failure(Exception(LicenseNumber.ERROR))
    if (password.length < 8) return@withContext Result.failure(Exception("Password must be at least 8 characters."))

    val remoteAccount = syncManager?.signupAgentOnBackend(
      name = cleanName,
      email = cleanEmail,
      licenseNo = cleanLicense,
      agency = cleanAgency,
      professionalRole = cleanRole,
      password = password
    )
    if (remoteAccount != null) {
      if (remoteAccount.isFailure) return@withContext Result.failure(remoteAccount.exceptionOrNull()!!)
      val account = remoteAccount.getOrThrow()
      database.userSessionDao().clearActiveSessions()
      database.userSessionDao().insertSession(UserSessionEntity.fromDomain(account))
      return@withContext Result.success(account)
    }

    database.userSessionDao().clearActiveSessions()
    val agentAccount = UserAccount(
      id = "agent_${UUID.randomUUID().toString().take(8)}",
      name = cleanName,
      email = cleanEmail,
      phone = "+63 917 555 0192",
      role = UserRole.AGENT,
      licenseNo = cleanLicense,
      agency = cleanAgency,
      professionalRole = cleanRole
    )
    database.userSessionDao().insertSession(UserSessionEntity.fromDomain(agentAccount))

    Result.success(agentAccount)
  }

  suspend fun logout() = withContext(Dispatchers.IO) {
    syncManager?.clearAuthToken()
    database.userSessionDao().clearActiveSessions()
  }

  suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
    val session = database.userSessionDao().findActiveSession()
      ?: return@withContext Result.failure(Exception("No active user session."))
    val result = syncManager?.updatePasswordOnBackend(session.email, currentPassword, newPassword)
    if (result == null) {
      Result.success(Unit)
    } else if (result.isFailure) {
      Result.failure(result.exceptionOrNull()!!)
    } else {
      Result.success(Unit)
    }
  }

  suspend fun resetDatabaseToDefaults() = withContext(Dispatchers.IO) {
    syncManager?.clearAuthToken()
    database.userSessionDao().clearAll()
    database.propertyDao().clearAll()
    database.appointmentDao().clearAll()
    database.inquiryDao().clearAll()
    database.notificationDao().clearAll()
    seedDatabase()
  }

  suspend fun addNotification(
    title: String,
    message: String,
    type: NotificationType,
    targetId: String? = null
  ) = withContext(Dispatchers.IO) {
    val notif = NotificationItem(
      id = UUID.randomUUID().toString(),
      title = title,
      message = message,
      timestampFormatted = "Just now",
      type = type,
      isRead = false,
      targetId = targetId
    )
    database.notificationDao().insertNotification(NotificationEntity.fromDomain(notif))
    syncManager?.pushNotification(notif)
  }

  suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
    DatabaseStats(
      propertiesCount = database.propertyDao().getCount(),
      appointmentsCount = database.appointmentDao().getCount(),
      inquiriesCount = database.inquiryDao().getCount(),
      notificationsCount = database.notificationDao().getCount()
    )
  }

  suspend fun testBackendConnection(showConnecting: Boolean = true) = syncManager?.testConnection(showConnecting)

  suspend fun syncWithBackend() = syncManager?.syncWithBackend()

  private suspend fun seedDatabase() = withContext(Dispatchers.IO) {
    database.agentProfileDao().insertOrUpdateProfile(AgentProfileEntity.fromDomain(AgentProfile()))
  }

  companion object {
    @Volatile
    private var INSTANCE: EstateRepository? = null

    fun getInstance(context: Context): EstateRepository {
      return INSTANCE ?: synchronized(this) {
        val database = EstateDatabase.getDatabase(context)
        val syncManager = BackendSyncManager.getInstance(context)
        val instance = EstateRepository(database, syncManager)
        INSTANCE = instance
        instance
      }
    }

    fun createForTesting(database: EstateDatabase, scope: CoroutineScope): EstateRepository {
      return EstateRepository(database, null, scope)
    }
  }
}

data class DatabaseStats(
  val propertiesCount: Int,
  val appointmentsCount: Int,
  val inquiriesCount: Int,
  val notificationsCount: Int
)

data class PropertyDeleteResult(
  val success: Boolean,
  val title: String,
  val backendDeleted: Boolean,
  val backendError: String? = null
)
