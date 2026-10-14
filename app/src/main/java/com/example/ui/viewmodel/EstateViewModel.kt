package com.example.ui.viewmodel

import com.example.data.model.ListingLimits
import com.example.data.model.ListingStatus
import com.example.data.model.upcomingViewingDates
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgentProfile
import com.example.data.model.AppointmentStatus
import com.example.data.model.Inquiry
import com.example.data.model.NotificationItem
import com.example.data.model.NotificationType
import com.example.data.model.Property
import com.example.data.model.UserAccount
import com.example.data.model.ViewingAppointment
import com.example.data.repository.DatabaseStats
import com.example.data.repository.EstateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

typealias UserRole = com.example.data.model.UserRole

enum class Screen {
  WELCOME,
  HOME,
  PROPERTY_DETAILS,
  AGENT_DASHBOARD,
  SAVED,
  SEARCH,
  MESSAGES,
  NOTIFICATIONS,
  CALENDAR_APPOINTMENTS,
  PROFILE
}

private data class FilterCriteria(
  val query: String,
  val category: String,
  val minPrice: Long,
  val maxPrice: Long,
  val minBeds: Double,
  val minBaths: Double,
  val minSqft: Int,
  val maxSqft: Int,
  val minYearBuilt: Int,
  val listingType: String,
  val selectedAmenities: Set<String>,
  val hideSold: Boolean
)

data class EstateUiState(
  val currentScreen: Screen = Screen.WELCOME,
  val previousScreen: Screen = Screen.HOME,
  val userRole: UserRole = UserRole.BUYER,
  val selectedProperty: Property? = null,
  val selectedInquiryId: String? = null,
  val searchQuery: String = "",
  val selectedCategory: String = "All",
  val minPrice: Long = 0L,
  val maxPrice: Long = 50000000L,
  val minBeds: Double = 0.0,
  val minBaths: Double = 0.0,
  val minSqft: Int = 0,
  val maxSqft: Int = 10000,
  val minYearBuilt: Int = 0,
  val listingType: String = "All", // "All", "For Sale", "For Rent"
  val selectedAmenities: Set<String> = emptySet(),
  val isFilterSheetOpen: Boolean = false,
  val isAddPropertyDialogOpen: Boolean = false,
  val isEditPropertyDialogOpen: Boolean = false,
  val propertyToEdit: Property? = null,
  val isDeletePropertyDialogOpen: Boolean = false,
  val propertyToDelete: Property? = null,
  val isBookingDialogOpen: Boolean = false,
  val isBookingSuccessDialogOpen: Boolean = false,
  val isClientAuthDialogOpen: Boolean = false,
  val isAgentLoginDialogOpen: Boolean = false,
  val pendingBookingProperty: Property? = null,
  val authError: String? = null,
  /** Friendly reason shown on the sign-in dialog, e.g. "Sign in to save homes". */
  val authPrompt: String? = null,
  val lastBookedAppointment: ViewingAppointment? = null,
  val bookingSelectedDate: String = upcomingViewingDates().first(),
  val bookingSelectedTime: String = "1:30 PM",
  val bookingClientName: String = "",
  val bookingClientPhone: String = "",
  val bookingClientEmail: String = "",
  val bookingNotes: String = "",
  val isInquiryDialogOpen: Boolean = false,
  val activeVideoUrl: String? = null,
  val activeVideoTitle: String = "",
  val isPublishingProperty: Boolean = false,
  val isRefreshing: Boolean = false,
  val isSavingProperty: Boolean = false,
  val isDeletingProperty: Boolean = false,
  val isSubmittingBooking: Boolean = false,
  val isSendingInquiry: Boolean = false,
  val isResettingDatabase: Boolean = false
) {
  val activeFilterCount: Int
    get() {
      var count = 0
      if (minPrice > 0L || maxPrice < 50000000L) count++
      if (minBeds > 0.0) count++
      if (minBaths > 0.0) count++
      if (minSqft > 0 || maxSqft < 10000) count++
      if (minYearBuilt > 0) count++
      if (listingType != "All") count++
      if (selectedCategory != "All") count++
      if (selectedAmenities.isNotEmpty()) count += selectedAmenities.size
      return count
    }
}

data class FeedbackEvent(
  val message: String,
  val actionLabel: String? = null,
  val onAction: (() -> Unit)? = null
)

class EstateViewModel(
  application: Application,
  private val repository: EstateRepository
) : AndroidViewModel(application) {

  constructor(application: Application) : this(
    application,
    EstateRepository.getInstance(application)
  )

  private val _feedbackChannel = kotlinx.coroutines.channels.Channel<FeedbackEvent>(kotlinx.coroutines.channels.Channel.BUFFERED)
  val feedbackEvents = _feedbackChannel.receiveAsFlow()

  fun postFeedback(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    viewModelScope.launch {
      _feedbackChannel.send(FeedbackEvent(message, actionLabel, onAction))
    }
  }

  private val _uiState = MutableStateFlow(EstateUiState())
  val uiState: StateFlow<EstateUiState> = _uiState.asStateFlow()

  val properties: StateFlow<List<Property>> = repository.properties
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Buyers only see their own bookings and conversations, even if the server sends everyone's.
  // Agents only see the ones for listings they posted (the listing's agent email is theirs).
  // Signed-out users see none.
  private fun agentListingIds(properties: List<Property>, user: UserAccount): Set<String> =
    properties.filter { it.agentEmail.trim().equals(user.email.trim(), ignoreCase = true) }
      .map { it.id }
      .toSet()

  val appointments: StateFlow<List<ViewingAppointment>> = combine(
    repository.appointments,
    repository.activeSession,
    repository.properties
  ) { list, user, properties ->
    when {
      user == null -> emptyList()
      user.role == UserRole.AGENT -> {
        val mine = agentListingIds(properties, user)
        list.filter { it.propertyId in mine }
      }
      else -> list.filter { it.clientEmail.trim().equals(user.email.trim(), ignoreCase = true) }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val inquiries: StateFlow<List<Inquiry>> = combine(
    repository.inquiries,
    repository.activeSession,
    repository.properties
  ) { list, user, properties ->
    when {
      user == null -> emptyList()
      user.role == UserRole.AGENT -> {
        val mine = agentListingIds(properties, user)
        list.filter { it.propertyId in mine }
      }
      else -> list.filter { it.senderEmail.trim().equals(user.email.trim(), ignoreCase = true) }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val notifications: StateFlow<List<NotificationItem>> = repository.notifications
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val agentProfile: StateFlow<AgentProfile> = repository.agentProfile
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AgentProfile())

  val currentUser: StateFlow<UserAccount?> = repository.activeSession
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  private val _databaseStats = MutableStateFlow<DatabaseStats?>(null)
  val databaseStats: StateFlow<DatabaseStats?> = _databaseStats.asStateFlow()

  val selectedInquiry: StateFlow<Inquiry?> = combine(
    inquiries,
    _uiState.map { it.selectedInquiryId }.distinctUntilChanged()
  ) { inqList, id ->
    if (id == null) null else inqList.find { it.id == id }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  val unreadNotificationsCount: StateFlow<Int> = notifications
    .map { notifs -> notifs.count { !it.isRead } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  private val filterCriteria = _uiState.map {
    FilterCriteria(
      query = it.searchQuery,
      category = it.selectedCategory,
      minPrice = it.minPrice,
      maxPrice = it.maxPrice,
      minBeds = it.minBeds,
      minBaths = it.minBaths,
      minSqft = it.minSqft,
      maxSqft = it.maxSqft,
      minYearBuilt = it.minYearBuilt,
      listingType = it.listingType,
      selectedAmenities = it.selectedAmenities,
      // Buyers don't need sold listings in search; agents still see all of theirs
      hideSold = it.userRole != UserRole.AGENT
    )
  }.distinctUntilChanged()

  val filteredProperties: StateFlow<List<Property>> = combine(
    properties,
    filterCriteria
  ) { list, criteria ->
    list.filter { prop ->
      val matchesCategory = when (criteria.category) {
        "All" -> true
        "Houses" -> prop.propertyType.equals("Houses", ignoreCase = true) || prop.propertyType.equals("House", ignoreCase = true)
        "Apartments" -> prop.propertyType.equals("Apartments", ignoreCase = true) || prop.propertyType.equals("Apartment", ignoreCase = true)
        "Condos" -> prop.propertyType.equals("Condos", ignoreCase = true) || prop.propertyType.equals("Condo", ignoreCase = true) || prop.propertyType.equals("Loft", ignoreCase = true)
        "Townhouses" -> prop.propertyType.equals("Townhouses", ignoreCase = true) || prop.propertyType.equals("Townhouse", ignoreCase = true)
        else -> prop.propertyType.contains(criteria.category, ignoreCase = true)
      }

      val matchesSearch = if (criteria.query.isBlank()) {
        true
      } else {
        val query = criteria.query.trim().lowercase()
        prop.title.lowercase().contains(query) ||
            prop.address.lowercase().contains(query) ||
            prop.cityStateZip.lowercase().contains(query) ||
            prop.propertyType.lowercase().contains(query)
      }

      val matchesBeds = prop.beds >= criteria.minBeds
      val matchesBaths = prop.baths >= criteria.minBaths
      // A slider at its top value means "no upper limit", so bigger or pricier listings still show
      val matchesSqft = prop.sqft >= criteria.minSqft &&
          (criteria.maxSqft >= ListingLimits.FILTER_MAX_SQFT || prop.sqft <= criteria.maxSqft)
      val matchesPrice = prop.price >= criteria.minPrice &&
          (criteria.maxPrice >= ListingLimits.FILTER_MAX_PRICE || prop.price <= criteria.maxPrice)
      val matchesYearBuilt = criteria.minYearBuilt <= 0 || prop.yearBuilt >= criteria.minYearBuilt
      val matchesListingType = when (criteria.listingType) {
        "For Sale" -> !prop.isRental
        "For Rent" -> prop.isRental
        else -> true
      }
      val matchesAmenities = if (criteria.selectedAmenities.isEmpty()) {
        true
      } else {
        criteria.selectedAmenities.all { selected ->
          prop.amenities.any { amenity -> amenity.contains(selected, ignoreCase = true) }
        }
      }

      val matchesStatus = !criteria.hideSold || !ListingStatus.isSold(prop.status)

      matchesStatus && matchesCategory && matchesSearch && matchesBeds && matchesBaths && matchesSqft && matchesPrice && matchesYearBuilt && matchesListingType && matchesAmenities
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val savedProperties: StateFlow<List<Property>> = properties
    .map { list -> list.filter { it.isFavorite } }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  init {
    refreshDatabaseStats()
    syncWithBackend(showProgress = false)
    viewModelScope.launch {
      currentUser.collect { user ->
        _uiState.update { state ->
          state.copy(
            userRole = user?.role ?: UserRole.BUYER,
            bookingClientName = user?.name ?: "",
            bookingClientEmail = user?.email ?: "",
            bookingClientPhone = user?.phone ?: ""
          )
        }
      }
    }
  }

  fun refreshDatabaseStats() {
    viewModelScope.launch {
      _databaseStats.value = repository.getDatabaseStats()
    }
  }

  fun navigateTo(screen: Screen) {
    _uiState.update { it.copy(currentScreen = screen) }
  }

  fun switchUserRole(role: UserRole) {
    if (role == UserRole.AGENT) {
      val user = currentUser.value
      if (user == null || user.role != UserRole.AGENT) {
        _uiState.update { it.copy(isAgentLoginDialogOpen = true, authError = "Agent credentials required to access Agent Portal.") }
        return
      }
    }
    _uiState.update {
      it.copy(
        userRole = role,
        currentScreen = if (role == UserRole.AGENT) Screen.AGENT_DASHBOARD else Screen.HOME
      )
    }
  }

  fun toggleUserRole() {
    val targetRole = if (_uiState.value.userRole == UserRole.BUYER) UserRole.AGENT else UserRole.BUYER
    if (targetRole == UserRole.AGENT) {
      val user = currentUser.value
      if (user == null || user.role != UserRole.AGENT) {
        _uiState.update { it.copy(isAgentLoginDialogOpen = true, authError = "Agent credentials required to access Agent Portal.") }
        postFeedback("Agent authentication required")
        return
      }
    }
    switchUserRole(targetRole)
    postFeedback(
      if (targetRole == UserRole.AGENT) "Switched to Agent Dashboard" else "Switched to Buyer Explorer Mode"
    )
  }

  fun openPropertyDetails(property: Property) {
    _uiState.update {
      it.copy(
        selectedProperty = property,
        previousScreen = it.currentScreen,
        currentScreen = Screen.PROPERTY_DETAILS,
        bookingSelectedDate = property.availableDates.firstOrNull() ?: upcomingViewingDates().first(),
        bookingSelectedTime = property.availableTimeSlots.firstOrNull() ?: "1:30 PM"
      )
    }
  }

  fun closePropertyDetails() {
    _uiState.update {
      it.copy(
        currentScreen = it.previousScreen,
        selectedProperty = null
      )
    }
  }

  fun toggleFavorite(propertyId: String, title: String? = null) {
    if (currentUser.value == null) {
      _uiState.update {
        it.copy(
          isClientAuthDialogOpen = true,
          authError = null,
          authPrompt = "Sign in or create an account to save homes."
        )
      }
      return
    }
    val prop = properties.value.find { it.id == propertyId } ?: _uiState.value.selectedProperty
    val wasFavorite = prop?.isFavorite ?: false
    repository.toggleFavorite(propertyId)
    _uiState.update { state ->
      if (state.selectedProperty?.id == propertyId) {
        state.copy(selectedProperty = state.selectedProperty.copy(isFavorite = !wasFavorite))
      } else {
        state
      }
    }

    val displayTitle = title ?: prop?.title
    val baseMsg = if (wasFavorite) "Removed from Saved" else "Saved to Favorites"
    val msg = if (displayTitle != null) "$baseMsg: $displayTitle" else baseMsg

    postFeedback(
      message = msg,
      actionLabel = "Undo",
      onAction = {
        repository.toggleFavorite(propertyId)
        _uiState.update { state ->
          if (state.selectedProperty?.id == propertyId) {
            state.copy(selectedProperty = state.selectedProperty.copy(isFavorite = wasFavorite))
          } else {
            state
          }
        }
        postFeedback(if (wasFavorite) "Restored to Saved" else "Removed from Saved")
      }
    )
  }

  fun setSelectedCategory(category: String) {
    _uiState.update { it.copy(selectedCategory = category) }
  }

  fun setSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun setFilters(
    minPrice: Long = 0L,
    maxPrice: Long = 50000000L,
    minBeds: Double = 0.0,
    minBaths: Double = 0.0,
    minSqft: Int = 0,
    maxSqft: Int = 10000,
    minYearBuilt: Int = 0,
    listingType: String = "All",
    selectedCategory: String? = null,
    selectedAmenities: Set<String> = emptySet()
  ) {
    _uiState.update {
      it.copy(
        minPrice = minPrice,
        maxPrice = maxPrice,
        minBeds = minBeds,
        minBaths = minBaths,
        minSqft = minSqft,
        maxSqft = maxSqft,
        minYearBuilt = minYearBuilt,
        listingType = listingType,
        selectedCategory = selectedCategory ?: it.selectedCategory,
        selectedAmenities = selectedAmenities,
        isFilterSheetOpen = false
      )
    }
    val isDefault = minPrice == 0L && maxPrice == 50000000L && minBeds == 0.0 &&
        minBaths == 0.0 && minSqft == 0 && maxSqft == 10000 && minYearBuilt == 0 &&
        listingType == "All" && selectedAmenities.isEmpty()

    if (isDefault) {
      postFeedback("Filters reset to default")
    } else {
      val count = (if (minPrice > 0L || maxPrice < 50000000L) 1 else 0) +
          (if (minBeds > 0.0) 1 else 0) +
          (if (minBaths > 0.0) 1 else 0) +
          (if (minSqft > 0 || maxSqft < 10000) 1 else 0) +
          (if (minYearBuilt > 0) 1 else 0) +
          (if (listingType != "All") 1 else 0) +
          selectedAmenities.size
      postFeedback("$count active filter(s) applied")
    }
  }

  fun resetFilters() {
    setFilters(
      minPrice = 0L,
      maxPrice = 50000000L,
      minBeds = 0.0,
      minBaths = 0.0,
      minSqft = 0,
      maxSqft = 10000,
      minYearBuilt = 0,
      listingType = "All",
      selectedCategory = "All",
      selectedAmenities = emptySet()
    )
  }

  fun clearPriceFilter() {
    _uiState.update { it.copy(minPrice = 0L, maxPrice = 50000000L) }
    postFeedback("Price filter cleared")
  }

  fun clearBedsFilter() {
    _uiState.update { it.copy(minBeds = 0.0) }
    postFeedback("Bedrooms filter cleared")
  }

  fun clearBathsFilter() {
    _uiState.update { it.copy(minBaths = 0.0) }
    postFeedback("Bathrooms filter cleared")
  }

  fun clearSqftFilter() {
    _uiState.update { it.copy(minSqft = 0, maxSqft = 10000) }
    postFeedback("Area size filter cleared")
  }

  fun clearYearBuiltFilter() {
    _uiState.update { it.copy(minYearBuilt = 0) }
    postFeedback("Year built filter cleared")
  }

  fun clearListingTypeFilter() {
    _uiState.update { it.copy(listingType = "All") }
    postFeedback("Listing type filter cleared")
  }

  fun removeAmenityFilter(amenity: String) {
    _uiState.update { it.copy(selectedAmenities = it.selectedAmenities - amenity) }
    postFeedback("Removed amenity filter: $amenity")
  }

  fun toggleAmenityFilter(amenity: String) {
    _uiState.update {
      val current = it.selectedAmenities
      val updated = if (current.contains(amenity)) current - amenity else current + amenity
      it.copy(selectedAmenities = updated)
    }
  }

  fun applyQuickPreset(preset: String) {
    when (preset) {
      "For Sale" -> _uiState.update { it.copy(listingType = "For Sale") }
      "For Rent" -> _uiState.update { it.copy(listingType = "For Rent") }
      "1+ Beds" -> _uiState.update { it.copy(minBeds = 1.0) }
      "2+ Beds" -> _uiState.update { it.copy(minBeds = 2.0) }
      "3+ Beds" -> _uiState.update { it.copy(minBeds = 3.0) }
      "4+ Beds" -> _uiState.update { it.copy(minBeds = 4.0) }
      "1+ Baths" -> _uiState.update { it.copy(minBaths = 1.0) }
      "2+ Baths" -> _uiState.update { it.copy(minBaths = 2.0) }
      "3+ Baths" -> _uiState.update { it.copy(minBaths = 3.0) }
      "1,000+ sqft" -> _uiState.update { it.copy(minSqft = 1000) }
      "2,000+ sqft" -> _uiState.update { it.copy(minSqft = 2000) }
      "Under ₱10M" -> _uiState.update { it.copy(maxPrice = 10000000L) }
      "Under ₱20M" -> _uiState.update { it.copy(maxPrice = 20000000L) }
      "Built 2020+" -> _uiState.update { it.copy(minYearBuilt = 2020) }
      "With Pool" -> _uiState.update { it.copy(selectedAmenities = it.selectedAmenities + "Pool") }
      "Sea View" -> _uiState.update { it.copy(selectedAmenities = it.selectedAmenities + "Sea View") }
      "Balcony" -> _uiState.update { it.copy(selectedAmenities = it.selectedAmenities + "Balcony") }
      "Gym" -> _uiState.update { it.copy(selectedAmenities = it.selectedAmenities + "Gym") }
    }
    postFeedback("Applied filter: $preset")
  }

  fun openFilterSheet(open: Boolean) {
    _uiState.update { it.copy(isFilterSheetOpen = open) }
  }

  fun openAddPropertyDialog(open: Boolean) {
    if (open) {
      val user = currentUser.value
      if (user == null || user.role != UserRole.AGENT) {
        _uiState.update { it.copy(isAgentLoginDialogOpen = true, authError = "Agent credentials required to publish listings.") }
        return
      }
    }
    _uiState.update { it.copy(isAddPropertyDialogOpen = open) }
  }

  fun initiateBooking(property: Property) {
    val user = currentUser.value
    val currentSelectedDate = _uiState.value.bookingSelectedDate
    val currentSelectedTime = _uiState.value.bookingSelectedTime

    val validDate = if (property.availableDates.contains(currentSelectedDate)) {
      currentSelectedDate
    } else {
      property.availableDates.firstOrNull() ?: currentSelectedDate.ifBlank { upcomingViewingDates().first() }
    }

    val validTime = if (property.availableTimeSlots.contains(currentSelectedTime)) {
      currentSelectedTime
    } else {
      property.availableTimeSlots.firstOrNull() ?: currentSelectedTime.ifBlank { "1:30 PM" }
    }

    if (user == null || user.role != UserRole.BUYER) {
      // Must login or sign up first before booking
      _uiState.update {
        it.copy(
          isClientAuthDialogOpen = true,
          pendingBookingProperty = property,
          selectedProperty = property,
          bookingSelectedDate = validDate,
          bookingSelectedTime = validTime,
          authError = null,
          authPrompt = "Sign in or create an account to book a viewing."
        )
      }
    } else {
      // Logged in as buyer -> show booking modal
      _uiState.update {
        it.copy(
          selectedProperty = property,
          isBookingDialogOpen = true,
          bookingClientName = user.name,
          bookingClientEmail = user.email,
          bookingClientPhone = user.phone,
          bookingSelectedDate = validDate,
          bookingSelectedTime = validTime
        )
      }
    }
  }

  fun openClientAuthDialog(open: Boolean, continueWithBookingProperty: Property? = null, prompt: String? = null) {
    _uiState.update {
      it.copy(
        isClientAuthDialogOpen = open,
        pendingBookingProperty = continueWithBookingProperty ?: it.pendingBookingProperty,
        authError = null,
        authPrompt = prompt
      )
    }
  }

  fun openAgentLoginDialog(open: Boolean) {
    _uiState.update {
      it.copy(
        isAgentLoginDialogOpen = open,
        authError = null
      )
    }
  }

  fun loginClient(email: String, password: String = "") {
    if (email.isBlank() || password.isBlank()) {
      _uiState.update { it.copy(authError = "Please enter both email and password.") }
      return
    }
    viewModelScope.launch {
      val result = repository.loginClient(email, password)
      if (result.isSuccess) {
        val user = result.getOrThrow()
        val pendingProperty = _uiState.value.pendingBookingProperty
        _uiState.update {
          it.copy(
            isClientAuthDialogOpen = false,
            authError = null,
            authPrompt = null,
            bookingClientName = user.name,
            bookingClientEmail = user.email,
            bookingClientPhone = user.phone,
            isBookingDialogOpen = pendingProperty != null,
            selectedProperty = pendingProperty ?: it.selectedProperty,
            pendingBookingProperty = null
          )
        }
        postFeedback("Signed in as ${user.name}")
        refreshDatabaseStats()
      } else {
        _uiState.update {
          it.copy(authError = result.exceptionOrNull()?.message ?: "Account not found or invalid credentials. Please create an account first.")
        }
      }
    }
  }

  fun signUpClient(name: String, email: String, phone: String, password: String = "") {
    if (name.isBlank() || email.isBlank() || password.length < 8) {
      _uiState.update { it.copy(authError = "Full name, email, and password of at least 8 characters are required.") }
      return
    }
    viewModelScope.launch {
      val result = repository.signUpClient(name, email, phone, password)
      if (result.isSuccess) {
        val user = result.getOrThrow()
        val pendingProperty = _uiState.value.pendingBookingProperty
        _uiState.update {
          it.copy(
            isClientAuthDialogOpen = false,
            authError = null,
            authPrompt = null,
            bookingClientName = user.name,
            bookingClientEmail = user.email,
            bookingClientPhone = user.phone,
            isBookingDialogOpen = pendingProperty != null,
            selectedProperty = pendingProperty ?: it.selectedProperty,
            pendingBookingProperty = null
          )
        }
        postFeedback("Welcome, ${user.name}! Your account is ready")
        refreshDatabaseStats()
      } else {
        _uiState.update {
          it.copy(authError = result.exceptionOrNull()?.message ?: "Sign up failed.")
        }
      }
    }
  }

  fun loginAgent(emailOrLicense: String, password: String) {
    viewModelScope.launch {
      val result = repository.loginAgent(emailOrLicense, password)
      if (result.isSuccess) {
        _uiState.update {
          it.copy(
            isAgentLoginDialogOpen = false,
            userRole = UserRole.AGENT,
            currentScreen = Screen.AGENT_DASHBOARD,
            authError = null
          )
        }
        postFeedback("Signed in to the Agent Portal")
      } else {
        _uiState.update {
          it.copy(authError = result.exceptionOrNull()?.message ?: "Invalid agent credentials")
        }
      }
      refreshDatabaseStats()
    }
  }

  fun signUpAgent(
    name: String,
    email: String,
    licenseNo: String,
    agency: String,
    professionalRole: String,
    password: String
  ) {
    viewModelScope.launch {
      val result = repository.signUpAgent(
        name = name,
        email = email,
        licenseNo = licenseNo,
        agency = agency,
        professionalRole = professionalRole,
        password = password
      )
      if (result.isSuccess) {
        _uiState.update {
          it.copy(
            isAgentLoginDialogOpen = false,
            userRole = UserRole.AGENT,
            currentScreen = Screen.AGENT_DASHBOARD,
            authError = null
          )
        }
        postFeedback("Agent account created & verified")
      } else {
        _uiState.update {
          it.copy(authError = result.exceptionOrNull()?.message ?: "Failed to create agent account")
        }
      }
      refreshDatabaseStats()
    }
  }

  fun logout() {
    viewModelScope.launch {
      repository.logout()
      _uiState.update {
        it.copy(
          userRole = UserRole.BUYER,
          currentScreen = Screen.HOME,
          isAgentLoginDialogOpen = false,
          isClientAuthDialogOpen = false,
          authError = null
        )
      }
      postFeedback("Signed out")
      refreshDatabaseStats()
    }
  }

  fun openBookingDialog(open: Boolean) {
    if (open && !selectedPropertyAcceptsBuyers()) return
    _uiState.update { it.copy(isBookingDialogOpen = open) }
  }

  /** False when a buyer is on a Pending or Sold listing, after telling them why. */
  private fun selectedPropertyAcceptsBuyers(): Boolean {
    val property = _uiState.value.selectedProperty ?: return true
    if (_uiState.value.userRole == UserRole.AGENT || ListingStatus.acceptsBuyers(property.status)) return true
    postFeedback(ListingStatus.buyerNote(property.status) ?: "This property is no longer available.")
    return false
  }

  fun openBookingSuccessDialog(open: Boolean) {
    _uiState.update { it.copy(isBookingSuccessDialogOpen = open) }
  }

  fun openInquiryDialog(open: Boolean) {
    if (open && !selectedPropertyAcceptsBuyers()) return
    if (open) {
      val user = currentUser.value
      if (user == null || user.role != UserRole.BUYER) {
        _uiState.update {
          it.copy(
            isClientAuthDialogOpen = true,
            authError = null,
            authPrompt = "Sign in or create an account to message the agent."
          )
        }
        return
      }
    }
    _uiState.update { it.copy(isInquiryDialogOpen = open) }
  }

  fun setBookingDate(date: String) {
    _uiState.update { it.copy(bookingSelectedDate = date) }
  }

  fun setBookingTime(time: String) {
    _uiState.update { it.copy(bookingSelectedTime = time) }
  }

  fun setBookingForm(name: String, phone: String, email: String, notes: String) {
    _uiState.update {
      it.copy(
        bookingClientName = name,
        bookingClientPhone = phone,
        bookingClientEmail = email,
        bookingNotes = notes
      )
    }
  }

  fun confirmBooking() {
    val user = currentUser.value
    if (user == null || user.role != UserRole.BUYER) {
      _uiState.update {
        it.copy(
          isBookingDialogOpen = false,
          isClientAuthDialogOpen = true,
          authError = "Client account required to schedule a viewing appointment."
        )
      }
      return
    }
    val state = _uiState.value
    val property = state.selectedProperty ?: return

    if (state.isSubmittingBooking) return
    _uiState.update { it.copy(isSubmittingBooking = true) }
    viewModelScope.launch {
      val appt = repository.bookAppointment(
        propertyId = property.id,
        date = state.bookingSelectedDate,
        timeSlot = state.bookingSelectedTime,
        clientName = state.bookingClientName.ifBlank { user.name },
        clientPhone = state.bookingClientPhone.ifBlank { user.phone },
        clientEmail = state.bookingClientEmail.ifBlank { user.email },
        notes = state.bookingNotes
      )

      _uiState.update {
        it.copy(
          isBookingDialogOpen = false,
          isBookingSuccessDialogOpen = true,
          isSubmittingBooking = false,
          lastBookedAppointment = appt
        )
      }
      postFeedback("Viewing scheduled for ${appt.date} at ${appt.timeSlot}")
      refreshDatabaseStats()
    }
  }

  fun submitInquiry(messageText: String) {
    val user = currentUser.value
    if (user == null || user.role != UserRole.BUYER) {
      _uiState.update {
        it.copy(
          isInquiryDialogOpen = false,
          isClientAuthDialogOpen = true,
          authError = "Client account required to send inquiries."
        )
      }
      return
    }
    val state = _uiState.value
    val property = state.selectedProperty ?: return
    val senderName = user.name
    val senderEmail = user.email
    val senderPhone = if (user.phone.isNotBlank()) user.phone else "+63 917 555 0134"

    if (state.isSendingInquiry) return
    _uiState.update { it.copy(isSendingInquiry = true) }
    viewModelScope.launch {
      val inq = repository.sendInquiry(
        propertyId = property.id,
        messageText = messageText,
        senderName = senderName,
        senderEmail = senderEmail,
        senderPhone = senderPhone
      )
      _uiState.update {
        it.copy(
          isInquiryDialogOpen = false,
          isSendingInquiry = false,
          selectedInquiryId = inq.id,
          currentScreen = Screen.MESSAGES
        )
      }
      postFeedback("Inquiry sent to ${property.agentName}")
      refreshDatabaseStats()
    }
  }

  fun openInquiryChat(inquiry: Inquiry) {
    repository.markInquiryAsRead(inquiry.id)
    _uiState.update {
      it.copy(
        selectedInquiryId = inquiry.id,
        currentScreen = Screen.MESSAGES
      )
    }
    refreshDatabaseStats()
  }

  fun closeInquiryChat() {
    _uiState.update {
      it.copy(selectedInquiryId = null)
    }
  }

  fun sendChatMessage(inquiryId: String, text: String) {
    if (text.isBlank()) return
    val isAgent = _uiState.value.userRole == UserRole.AGENT
    viewModelScope.launch {
      repository.replyToInquiry(inquiryId, text, isAgent = isAgent)
      refreshDatabaseStats()
    }
  }

  fun markNotificationAsRead(id: String) {
    repository.markNotificationAsRead(id)
  }

  fun markAllNotificationsAsRead() {
    val unreadCount = notifications.value.count { !it.isRead }
    repository.markAllNotificationsAsRead()
    if (unreadCount > 0) {
      postFeedback("All notifications marked as read")
    }
  }

  fun addCustomProperty(
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
    availableDates: List<String> = emptyList(),
    availableTimeSlots: List<String> = emptyList()
  ) {
    if (_uiState.value.userRole != UserRole.AGENT) {
      return
    }
    if (_uiState.value.isPublishingProperty) return
    _uiState.update { it.copy(isPublishingProperty = true) }
    viewModelScope.launch {
      val created = repository.addProperty(
        title = title,
        address = address,
        cityStateZip = cityStateZip,
        price = price,
        isRental = isRental,
        beds = beds,
        baths = baths,
        sqft = sqft,
        propertyType = propertyType,
        description = description,
        amenities = amenities,
        mediaUris = mediaUris,
        availableDates = availableDates.ifEmpty { upcomingViewingDates() },
        availableTimeSlots = availableTimeSlots.ifEmpty { listOf("10:00 AM", "1:30 PM", "4:00 PM", "5:30 PM") }
      )
      _uiState.update {
        it.copy(
          isAddPropertyDialogOpen = false,
          isPublishingProperty = false
        )
      }
      postFeedback("Listing published")
      refreshDatabaseStats()
    }
  }

  fun openEditPropertyDialog(property: Property?) {
    if (property != null && _uiState.value.userRole != UserRole.AGENT) {
      return
    }
    _uiState.update {
      it.copy(
        isEditPropertyDialogOpen = property != null,
        propertyToEdit = property
      )
    }
  }

  fun closeEditPropertyDialog() {
    _uiState.update {
      it.copy(
        isEditPropertyDialogOpen = false,
        propertyToEdit = null
      )
    }
  }

  fun updateCustomProperty(property: Property) {
    if (_uiState.value.userRole != UserRole.AGENT) {
      return
    }
    if (_uiState.value.isSavingProperty) return
    _uiState.update { it.copy(isSavingProperty = true) }
    viewModelScope.launch {
      val updated = repository.updateProperty(property)
      _uiState.update { state ->
        state.copy(
          isEditPropertyDialogOpen = false,
          propertyToEdit = null,
          isSavingProperty = false,
          selectedProperty = if (state.selectedProperty?.id == updated.id) updated else state.selectedProperty
        )
      }
      postFeedback("Listing changes saved successfully")
      refreshDatabaseStats()
    }
  }

  fun openDeletePropertyDialog(property: Property?) {
    if (property != null && _uiState.value.userRole != UserRole.AGENT) {
      postFeedback("Only verified agents can delete listings")
      return
    }
    _uiState.update {
      it.copy(
        isDeletePropertyDialogOpen = property != null,
        propertyToDelete = property
      )
    }
  }

  fun closeDeletePropertyDialog() {
    _uiState.update {
      it.copy(
        isDeletePropertyDialogOpen = false,
        propertyToDelete = null
      )
    }
  }

  fun confirmDeleteProperty() {
    if (_uiState.value.userRole != UserRole.AGENT) {
      postFeedback("Only verified agents can delete listings")
      return
    }
    val prop = _uiState.value.propertyToDelete ?: return
    if (_uiState.value.isDeletingProperty) return
    _uiState.update { it.copy(isDeletingProperty = true) }
    viewModelScope.launch {
      val result = repository.deleteProperty(prop.id)
      _uiState.update { state ->
        val shouldNavigateHome = state.selectedProperty?.id == prop.id
        state.copy(
          isDeletePropertyDialogOpen = false,
          propertyToDelete = null,
          isDeletingProperty = false,
          selectedProperty = if (shouldNavigateHome) null else state.selectedProperty,
          currentScreen = if (shouldNavigateHome) (if (state.userRole == UserRole.AGENT) Screen.AGENT_DASHBOARD else Screen.HOME) else state.currentScreen
        )
      }
      if (result.backendDeleted) {
        postFeedback("Listing \"${prop.title}\" deleted")
      } else if (result.backendError != null) {
        postFeedback("Removed from this device, but it couldn't be removed online. Please try again later.")
      } else {
        postFeedback("Listing \"${prop.title}\" deleted")
      }
      refreshDatabaseStats()
    }
  }

  fun playVideo(videoUrl: String, title: String = "Property Tour") {
    _uiState.update {
      it.copy(
        activeVideoUrl = videoUrl,
        activeVideoTitle = title
      )
    }
  }

  fun closeVideo() {
    _uiState.update {
      it.copy(
        activeVideoUrl = null,
        activeVideoTitle = ""
      )
    }
  }

  fun updateAppointmentStatus(appointmentId: String, status: AppointmentStatus, clientName: String? = null) {
    val previousStatus = appointments.value.find { it.id == appointmentId }?.status ?: AppointmentStatus.UPCOMING
    repository.updateAppointmentStatus(appointmentId, status)
    refreshDatabaseStats()

    val msg = when (status) {
      AppointmentStatus.COMPLETED -> if (clientName != null) "Viewing with $clientName marked as completed" else "Viewing marked as completed"
      AppointmentStatus.CANCELLED -> "Viewing appointment cancelled"
      AppointmentStatus.UPCOMING -> "Viewing appointment scheduled"
      AppointmentStatus.PENDING -> "Viewing status updated to pending"
    }

    postFeedback(
      message = msg,
      actionLabel = if (status == AppointmentStatus.COMPLETED) "Undo" else null,
      onAction = if (status == AppointmentStatus.COMPLETED) {
        {
          repository.updateAppointmentStatus(appointmentId, previousStatus)
          refreshDatabaseStats()
          postFeedback("Viewing restored to upcoming")
        }
      } else null
    )
  }

  fun resetDatabaseToDefaults() {
    _uiState.update { it.copy(isResettingDatabase = true) }
    viewModelScope.launch {
      repository.resetDatabaseToDefaults()
      _uiState.update { it.copy(isResettingDatabase = false) }
      postFeedback("Database restored to demo defaults")
      refreshDatabaseStats()
    }
  }

  fun updateUserPassword(currentPassword: String, newPassword: String) {
    viewModelScope.launch {
      val result = repository.updatePassword(currentPassword, newPassword)
      if (result.isSuccess) {
        repository.addNotification(
          title = "Security Alert: Password Updated",
          message = "Your account password was changed successfully.",
          type = NotificationType.SECURITY_ALERT
        )
        postFeedback("Password updated successfully")
      } else {
        postFeedback(result.exceptionOrNull()?.message ?: "Password update failed")
      }
      refreshDatabaseStats()
    }
  }

  val backendConnectionInfo: StateFlow<com.example.data.remote.BackendConnectionInfo?> = (repository.backendConnectionInfo ?: MutableStateFlow(null))
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.backendConnectionInfo?.value)

  /**
   * Re-checks status.php without a snackbar or a "Connecting" flicker. Called
   * periodically while the app is on screen to keep the status dot current.
   */
  fun refreshConnectionStatus() {
    viewModelScope.launch {
      repository.testBackendConnection(showConnecting = false)
    }
  }

  /** Pull-to-refresh: syncs everything quietly and only speaks up when it fails. */
  fun pullToRefresh() {
    if (_uiState.value.isRefreshing) return
    _uiState.update { it.copy(isRefreshing = true) }
    viewModelScope.launch {
      val result = repository.syncWithBackend()
      if (result != null && result.isSuccess) {
        refreshDatabaseStats()
      } else {
        postFeedback("Couldn't refresh right now. Please check your connection and try again.")
      }
      _uiState.update { it.copy(isRefreshing = false) }
    }
  }

  fun syncWithBackend(showProgress: Boolean = true) {
    viewModelScope.launch {
      if (showProgress) postFeedback("Syncing your listings...")
      val result = repository.syncWithBackend()
      if (result != null && result.isSuccess) {
        if (showProgress) postFeedback(result.getOrDefault("Everything is up to date"))
        refreshDatabaseStats()
      } else if (showProgress) {
        postFeedback("Couldn't sync right now. Please check your connection and try again.")
      }
    }
  }
}
