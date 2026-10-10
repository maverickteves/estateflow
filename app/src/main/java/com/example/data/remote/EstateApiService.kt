package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface EstateApiService {

  @GET("status.php")
  suspend fun getStatus(): Response<BackendStatusResponse>

  /**
   * Main sync endpoint - decodes /estateflow-backend/api/sync.php as SyncResponse object
   */
  @GET("sync.php")
  suspend fun sync(): SyncResponse

  @GET("sync.php")
  suspend fun getFullSyncState(): Response<SyncResponse>

  /**
   * Properties endpoint - returns a JSON array of properties
   */
  @GET("properties.php")
  suspend fun getProperties(
    @Query("search") search: String? = null,
    @Query("category") category: String? = null
  ): List<PropertyDto>

  @POST("properties.php")
  suspend fun createProperty(@Body property: PropertyDto): Response<PropertyDto>

  @PUT("properties.php?action=toggle_favorite")
  suspend fun toggleFavorite(@Query("id") id: String): Response<ApiResponse>

  @DELETE("properties.php")
  suspend fun deleteProperty(@Query("id") id: String): Response<ApiResponse>

  @POST("auth.php?action=client_login")
  suspend fun clientLogin(@Body payload: Map<String, String>): Response<AuthResponse>

  @POST("auth.php?action=client_signup")
  suspend fun clientSignup(@Body payload: Map<String, String>): Response<AuthResponse>

  @POST("auth.php?action=agent_login")
  suspend fun agentLogin(@Body payload: Map<String, String>): Response<AuthResponse>

  @POST("auth.php?action=agent_signup")
  suspend fun agentSignup(@Body payload: Map<String, String>): Response<AuthResponse>

  @POST("auth.php?action=update_password")
  suspend fun updatePassword(@Body payload: Map<String, String>): Response<ApiResponse>

  @POST("auth.php?action=update_profile")
  suspend fun updateProfile(@Body payload: Map<String, String>): Response<AgentProfileDto>

  /**
   * Appointments endpoint - returns a JSON array of appointments
   */
  @GET("appointments.php")
  suspend fun getAppointments(): List<ViewingAppointmentDto>

  @POST("appointments.php")
  suspend fun bookAppointment(@Body appointment: ViewingAppointmentDto): Response<ViewingAppointmentDto>

  @PUT("appointments.php")
  suspend fun updateAppointmentStatus(
    @Query("id") id: String,
    @Query("status") status: String
  ): Response<ApiResponse>

  /**
   * Inquiries endpoint - returns a JSON array of inquiries
   */
  @GET("inquiries.php")
  suspend fun getInquiries(): List<InquiryDto>

  @POST("inquiries.php")
  suspend fun sendInquiry(@Body inquiry: InquiryDto): Response<InquiryDto>

  @POST("inquiries.php?action=reply")
  suspend fun replyInquiry(@Body payload: Map<String, String>): Response<ApiResponse>

  @PUT("inquiries.php?action=mark_read")
  suspend fun markInquiryRead(@Query("id") id: String): Response<ApiResponse>

  /**
   * Notifications endpoint - returns a JSON array of notifications
   */
  @GET("notifications.php")
  suspend fun getNotifications(): List<NotificationDto>

  @PUT("notifications.php?action=mark_read")
  suspend fun markNotificationRead(@Query("id") id: String): Response<ApiResponse>

  @PUT("notifications.php?action=mark_all_read")
  suspend fun markAllNotificationsRead(): Response<ApiResponse>

  @POST("notifications.php")
  suspend fun createNotification(@Body notification: NotificationDto): Response<NotificationDto>

  @POST("sync.php")
  suspend fun postSync(@Body syncRequest: SyncRequestDto): Response<SyncResponse>
}
