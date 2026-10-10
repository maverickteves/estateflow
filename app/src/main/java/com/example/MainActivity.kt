package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppointmentStatus
import com.example.ui.components.EstateBottomNav
import com.example.ui.components.EstateTopBar
import com.example.ui.dialogs.AddPropertyDialog
import com.example.ui.dialogs.AgentLoginDialog
import com.example.ui.dialogs.BookingDialog
import com.example.ui.dialogs.BookingSuccessDialog
import com.example.ui.dialogs.ClientAuthDialog
import com.example.ui.dialogs.DeletePropertyConfirmationDialog
import com.example.ui.dialogs.EditPropertyDialog
import com.example.ui.dialogs.FilterBottomSheet
import com.example.ui.dialogs.InquiryDialog
import com.example.ui.dialogs.PropertyVideoPlayerDialog
import com.example.ui.screens.AgentDashboardScreen
import com.example.ui.screens.CalendarAppointmentsScreen
import com.example.ui.screens.HomeListingsScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PropertyDetailsScreen
import com.example.ui.screens.SavedScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NaturalBg
import com.example.ui.viewmodel.EstateViewModel
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.UserRole

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        EstateflowApp()
      }
    }
  }
}

@Composable
fun EstateflowApp(
  viewModel: EstateViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val properties by viewModel.properties.collectAsStateWithLifecycle()
  val filteredProperties by viewModel.filteredProperties.collectAsStateWithLifecycle()
  val savedProperties by viewModel.savedProperties.collectAsStateWithLifecycle()
  val appointments by viewModel.appointments.collectAsStateWithLifecycle()
  val inquiries by viewModel.inquiries.collectAsStateWithLifecycle()
  val notifications by viewModel.notifications.collectAsStateWithLifecycle()
  val agentProfile by viewModel.agentProfile.collectAsStateWithLifecycle()
  val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
  val selectedInquiry by viewModel.selectedInquiry.collectAsStateWithLifecycle()
  val databaseStats by viewModel.databaseStats.collectAsStateWithLifecycle()
  val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
  val backendConnectionInfo by viewModel.backendConnectionInfo.collectAsStateWithLifecycle()

  // Handle hardware back button
  BackHandler(enabled = uiState.currentScreen != Screen.HOME && uiState.currentScreen != Screen.AGENT_DASHBOARD && uiState.currentScreen != Screen.WELCOME) {
    if (uiState.currentScreen == Screen.PROPERTY_DETAILS) {
      viewModel.closePropertyDetails()
    } else if (selectedInquiry != null) {
      viewModel.closeInquiryChat()
      viewModel.navigateTo(Screen.MESSAGES)
    } else {
      val defaultScreen = if (uiState.userRole == UserRole.AGENT) Screen.AGENT_DASHBOARD else Screen.HOME
      viewModel.navigateTo(defaultScreen)
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      // Show top bar except on Welcome, PropertyDetails, AgentDashboard, and Notifications
      if (uiState.currentScreen != Screen.WELCOME && uiState.currentScreen != Screen.PROPERTY_DETAILS && uiState.currentScreen != Screen.AGENT_DASHBOARD && uiState.currentScreen != Screen.NOTIFICATIONS) {
        EstateTopBar(
          title = "Estateflow",
          subtitle = null,
          userRole = uiState.userRole,
          unreadCount = unreadNotificationsCount,
          onNotificationClick = { viewModel.navigateTo(Screen.NOTIFICATIONS) },
          onFilterClick = { viewModel.openFilterSheet(true) },
          onToggleRoleClick = { viewModel.toggleUserRole() },
          onProfileClick = { viewModel.navigateTo(Screen.PROFILE) }
        )
      }
    },
    bottomBar = {
      if (uiState.currentScreen != Screen.WELCOME) {
        EstateBottomNav(
          currentScreen = uiState.currentScreen,
          userRole = uiState.userRole,
          unreadMessagesCount = inquiries.count { it.isUnread },
          onNavigate = { screen -> viewModel.navigateTo(screen) }
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Crossfade(
        targetState = uiState.currentScreen,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "screen_crossfade"
      ) { screen ->
        when (screen) {
          Screen.WELCOME -> {
            WelcomeScreen(
              onGetStarted = { viewModel.navigateTo(Screen.HOME) },
              onAgentPortalClick = { viewModel.openAgentLoginDialog(true) }
            )
          }

          Screen.HOME -> {
            HomeListingsScreen(
              properties = filteredProperties,
              searchQuery = uiState.searchQuery,
              selectedCategory = uiState.selectedCategory,
              onSearchQueryChange = { viewModel.setSearchQuery(it) },
              onCategorySelect = { viewModel.setSelectedCategory(it) },
              onPropertyClick = { prop -> viewModel.openPropertyDetails(prop) },
              onFavoriteClick = { id -> viewModel.toggleFavorite(id) },
              onOpenFilterSheet = { viewModel.openFilterSheet(true) },
              activeFilterCount = uiState.activeFilterCount,
              minPrice = uiState.minPrice,
              maxPrice = uiState.maxPrice,
              minBeds = uiState.minBeds,
              minBaths = uiState.minBaths,
              minSqft = uiState.minSqft,
              maxSqft = uiState.maxSqft,
              minYearBuilt = uiState.minYearBuilt,
              listingType = uiState.listingType,
              selectedAmenities = uiState.selectedAmenities,
              onClearPriceFilter = { viewModel.clearPriceFilter() },
              onClearBedsFilter = { viewModel.clearBedsFilter() },
              onClearBathsFilter = { viewModel.clearBathsFilter() },
              onClearSqftFilter = { viewModel.clearSqftFilter() },
              onClearYearBuiltFilter = { viewModel.clearYearBuiltFilter() },
              onClearListingTypeFilter = { viewModel.clearListingTypeFilter() },
              onRemoveAmenityFilter = { amenity -> viewModel.removeAmenityFilter(amenity) },
              onResetFilters = { viewModel.resetFilters() },
              onApplyQuickPreset = { preset -> viewModel.applyQuickPreset(preset) },
              onAddPropertyClick = if (uiState.userRole == UserRole.AGENT) { { viewModel.openAddPropertyDialog(true) } } else null,
              onEditPropertyClick = if (uiState.userRole == UserRole.AGENT) { { prop -> viewModel.openEditPropertyDialog(prop) } } else null,
              onDeletePropertyClick = if (uiState.userRole == UserRole.AGENT) { { prop -> viewModel.openDeletePropertyDialog(prop) } } else null
            )
          }

          Screen.PROPERTY_DETAILS -> {
            uiState.selectedProperty?.let { property ->
              PropertyDetailsScreen(
                property = property,
                selectedDate = uiState.bookingSelectedDate,
                selectedTimeSlot = uiState.bookingSelectedTime,
                onDateSelected = { date -> viewModel.setBookingDate(date) },
                onTimeSlotSelected = { time -> viewModel.setBookingTime(time) },
                onBackClick = { viewModel.closePropertyDetails() },
                onFavoriteClick = { viewModel.toggleFavorite(property.id) },
                onBookAppointmentClick = { viewModel.initiateBooking(property) },
                onInquiryClick = { viewModel.openInquiryDialog(true) },
                onEditClick = if (uiState.userRole == UserRole.AGENT) { { viewModel.openEditPropertyDialog(property) } } else null,
                onDeleteClick = if (uiState.userRole == UserRole.AGENT) { { viewModel.openDeletePropertyDialog(property) } } else null,
                onPlayVideoClick = { videoUrl -> viewModel.playVideo(videoUrl, property.title) }
              )
            }
          }

          Screen.AGENT_DASHBOARD -> {
            AgentDashboardScreen(
              agentProfile = agentProfile,
              currentUser = currentUser,
              appointments = appointments,
              inquiries = inquiries,
              activeListingsCount = properties.size,
              properties = properties,
              onAddPropertyClick = { viewModel.openAddPropertyDialog(true) },
              onAvailabilityClick = { viewModel.navigateTo(Screen.CALENDAR_APPOINTMENTS) },
              onInquiriesClick = { viewModel.navigateTo(Screen.MESSAGES) },
              onEditListingsClick = { viewModel.navigateTo(Screen.HOME) },
              onAppointmentClick = { appt -> viewModel.navigateTo(Screen.CALENDAR_APPOINTMENTS) },
              onInquiryClick = { inq -> viewModel.openInquiryChat(inq) },
              onPropertyClick = { prop -> viewModel.openPropertyDetails(prop) },
              onEditPropertyClick = { prop -> viewModel.openEditPropertyDialog(prop) },
              onDeletePropertyClick = { prop -> viewModel.openDeletePropertyDialog(prop) },
              onPlayVideoClick = { videoUrl -> viewModel.playVideo(videoUrl, "Property Video Tour") }
            )
          }

          Screen.SAVED -> {
            SavedScreen(
              savedProperties = savedProperties,
              onPropertyClick = { prop -> viewModel.openPropertyDetails(prop) },
              onFavoriteClick = { id -> viewModel.toggleFavorite(id) },
              onExploreClick = { viewModel.navigateTo(Screen.HOME) }
            )
          }

          Screen.SEARCH -> {
            SearchScreen(
              properties = filteredProperties,
              searchQuery = uiState.searchQuery,
              selectedCategory = uiState.selectedCategory,
              onSearchQueryChange = { viewModel.setSearchQuery(it) },
              onCategorySelect = { viewModel.setSelectedCategory(it) },
              onPropertyClick = { prop -> viewModel.openPropertyDetails(prop) },
              onFavoriteClick = { id -> viewModel.toggleFavorite(id) },
              onOpenFilterSheet = { viewModel.openFilterSheet(true) },
              activeFilterCount = uiState.activeFilterCount,
              minPrice = uiState.minPrice,
              maxPrice = uiState.maxPrice,
              minBeds = uiState.minBeds,
              minBaths = uiState.minBaths,
              minSqft = uiState.minSqft,
              maxSqft = uiState.maxSqft,
              minYearBuilt = uiState.minYearBuilt,
              listingType = uiState.listingType,
              selectedAmenities = uiState.selectedAmenities,
              onClearPriceFilter = { viewModel.clearPriceFilter() },
              onClearBedsFilter = { viewModel.clearBedsFilter() },
              onClearBathsFilter = { viewModel.clearBathsFilter() },
              onClearSqftFilter = { viewModel.clearSqftFilter() },
              onClearYearBuiltFilter = { viewModel.clearYearBuiltFilter() },
              onClearListingTypeFilter = { viewModel.clearListingTypeFilter() },
              onRemoveAmenityFilter = { amenity -> viewModel.removeAmenityFilter(amenity) },
              onResetFilters = { viewModel.resetFilters() },
              onApplyQuickPreset = { preset -> viewModel.applyQuickPreset(preset) }
            )
          }

          Screen.MESSAGES -> {
            MessagesScreen(
              inquiries = inquiries,
              selectedInquiry = selectedInquiry,
              userRole = uiState.userRole,
              properties = properties,
              onSelectInquiry = { inq -> viewModel.openInquiryChat(inq) },
              onBackToList = {
                viewModel.closeInquiryChat()
              },
              onSendMessage = { inqId, text -> viewModel.sendChatMessage(inqId, text) }
            )
          }

          Screen.NOTIFICATIONS -> {
            NotificationsScreen(
              notifications = notifications,
              onNotificationClick = { notif ->
                viewModel.markNotificationAsRead(notif.id)
                val target = notif.targetId
                if (target != null) {
                  val matchedInq = inquiries.find { it.id == target }
                  val matchedAppt = appointments.find { it.id == target }
                  val matchedProp = filteredProperties.find { it.id == target }

                  when {
                    matchedInq != null -> viewModel.openInquiryChat(matchedInq)
                    matchedAppt != null -> viewModel.navigateTo(Screen.CALENDAR_APPOINTMENTS)
                    matchedProp != null -> viewModel.openPropertyDetails(matchedProp)
                    notif.type == com.example.data.model.NotificationType.VIEWING_BOOKED -> viewModel.navigateTo(Screen.CALENDAR_APPOINTMENTS)
                    notif.type == com.example.data.model.NotificationType.NEW_INQUIRY -> viewModel.navigateTo(Screen.MESSAGES)
                    else -> viewModel.navigateTo(Screen.HOME)
                  }
                }
              },
              onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() },
              onBackClick = {
                val defaultScreen = if (uiState.userRole == UserRole.AGENT) Screen.AGENT_DASHBOARD else Screen.HOME
                viewModel.navigateTo(defaultScreen)
              }
            )
          }

          Screen.CALENDAR_APPOINTMENTS -> {
            CalendarAppointmentsScreen(
              appointments = appointments,
              onUpdateStatus = { id, status -> viewModel.updateAppointmentStatus(id, status) }
            )
          }

          Screen.PROFILE -> {
            ProfileScreen(
              userRole = uiState.userRole,
              currentUser = currentUser,
              agentProfile = agentProfile,
              savedPropertiesCount = savedProperties.size,
              upcomingViewingsCount = appointments.count { it.status == AppointmentStatus.UPCOMING },
              databaseStats = databaseStats,
              backendConnectionInfo = backendConnectionInfo,
              onTestBackendConnection = { viewModel.testBackendConnection() },
              onSyncBackend = { viewModel.syncWithBackend() },
              onUpdateBackendUrl = { url -> viewModel.updateBackendUrl(url) },
              onOpenClientAuth = { viewModel.openClientAuthDialog(true) },
              onOpenAgentLogin = { viewModel.openAgentLoginDialog(true) },
              onLogout = { viewModel.logout() },
              onResetDatabase = { viewModel.resetDatabaseToDefaults() },
              onToggleRole = { viewModel.toggleUserRole() },
              onOpenNotifications = { viewModel.navigateTo(Screen.NOTIFICATIONS) },
              onUpdatePassword = { newPass -> viewModel.updateUserPassword(newPass) }
            )
          }
        }
      }
    }
  }

  // Dialog Overlays
  if (uiState.isBookingDialogOpen && uiState.selectedProperty != null) {
    BookingDialog(
      property = uiState.selectedProperty!!,
      selectedDate = uiState.bookingSelectedDate,
      selectedTimeSlot = uiState.bookingSelectedTime,
      clientName = uiState.bookingClientName,
      clientPhone = uiState.bookingClientPhone,
      clientEmail = uiState.bookingClientEmail,
      notes = uiState.bookingNotes,
      onNameChange = { viewModel.setBookingForm(it, uiState.bookingClientPhone, uiState.bookingClientEmail, uiState.bookingNotes) },
      onPhoneChange = { viewModel.setBookingForm(uiState.bookingClientName, it, uiState.bookingClientEmail, uiState.bookingNotes) },
      onEmailChange = { viewModel.setBookingForm(uiState.bookingClientName, uiState.bookingClientPhone, it, uiState.bookingNotes) },
      onNotesChange = { viewModel.setBookingForm(uiState.bookingClientName, uiState.bookingClientPhone, uiState.bookingClientEmail, it) },
      onDateSelected = { date -> viewModel.setBookingDate(date) },
      onTimeSlotSelected = { time -> viewModel.setBookingTime(time) },
      onConfirm = { viewModel.confirmBooking() },
      onDismiss = { viewModel.openBookingDialog(false) }
    )
  }

  if (uiState.isBookingSuccessDialogOpen) {
    BookingSuccessDialog(
      appointment = uiState.lastBookedAppointment,
      onDismiss = { viewModel.openBookingSuccessDialog(false) }
    )
  }

  if (uiState.isAddPropertyDialogOpen) {
    AddPropertyDialog(
      onDismiss = { viewModel.openAddPropertyDialog(false) },
      onAddProperty = { title, address, cityStateZip, price, isRental, beds, baths, sqft, propertyType, description, amenities, mediaUris, availableDates, availableTimeSlots ->
        viewModel.addCustomProperty(
          title, address, cityStateZip, price, isRental, beds, baths, sqft, propertyType, description, amenities, mediaUris, availableDates, availableTimeSlots
        )
      }
    )
  }

  if (uiState.isEditPropertyDialogOpen && uiState.propertyToEdit != null) {
    EditPropertyDialog(
      property = uiState.propertyToEdit!!,
      onDismiss = { viewModel.closeEditPropertyDialog() },
      onUpdateProperty = { updated -> viewModel.updateCustomProperty(updated) },
      onDeleteProperty = { viewModel.openDeletePropertyDialog(uiState.propertyToEdit!!) }
    )
  }

  if (uiState.isDeletePropertyDialogOpen && uiState.propertyToDelete != null) {
    DeletePropertyConfirmationDialog(
      property = uiState.propertyToDelete!!,
      onConfirmDelete = { viewModel.confirmDeleteProperty() },
      onDismiss = { viewModel.closeDeletePropertyDialog() }
    )
  }

  if (uiState.activeVideoUrl != null) {
    PropertyVideoPlayerDialog(
      videoUri = uiState.activeVideoUrl!!,
      title = uiState.activeVideoTitle.ifEmpty { "Property Video Tour" },
      onDismiss = { viewModel.closeVideo() }
    )
  }

  if (uiState.isFilterSheetOpen) {
    FilterBottomSheet(
      minPrice = uiState.minPrice,
      maxPrice = uiState.maxPrice,
      minBeds = uiState.minBeds,
      minBaths = uiState.minBaths,
      minSqft = uiState.minSqft,
      maxSqft = uiState.maxSqft,
      minYearBuilt = uiState.minYearBuilt,
      listingType = uiState.listingType,
      selectedCategory = uiState.selectedCategory,
      selectedAmenities = uiState.selectedAmenities,
      matchingCount = filteredProperties.size,
      onApplyFilters = { minP, maxP, beds, baths, minSq, maxSq, minYr, type, cat, amenities ->
        viewModel.setFilters(minP, maxP, beds, baths, minSq, maxSq, minYr, type, cat, amenities)
      },
      onResetFilters = { viewModel.resetFilters() },
      onDismiss = { viewModel.openFilterSheet(false) }
    )
  }

  if (uiState.isInquiryDialogOpen && uiState.selectedProperty != null) {
    InquiryDialog(
      property = uiState.selectedProperty!!,
      initialMessage = uiState.quickInquiryMessage,
      onSendMessage = { message: String ->
        viewModel.submitInquiry(message)
      },
      onDismiss = { viewModel.openInquiryDialog(false) }
    )
  }

  // Client Auth Dialog (Shown when booking without being logged in or from Profile)
  ClientAuthDialog(
    isOpen = uiState.isClientAuthDialogOpen,
    onDismiss = { viewModel.openClientAuthDialog(false) },
    onLogin = { email, password -> viewModel.loginClient(email, password) },
    onSignUp = { name, email, phone, password -> viewModel.signUpClient(name, email, phone, password) },
    authError = uiState.authError
  )

  // Dedicated Agent Portal Login & Registration Dialog
  AgentLoginDialog(
    isOpen = uiState.isAgentLoginDialogOpen,
    onDismiss = { viewModel.openAgentLoginDialog(false) },
    onLoginAgent = { emailOrLicense, password -> viewModel.loginAgent(emailOrLicense, password) },
    onSignUpAgent = { name, email, licenseNo, agency, role, password ->
      viewModel.signUpAgent(name, email, licenseNo, agency, role, password)
    },
    authError = uiState.authError
  )
}
