package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EstateDatabase
import com.example.data.local.entity.PropertyEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Estateflow", appName)
  }

  @Test
  fun `room database stores and retrieves property`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, EstateDatabase::class.java).build()
    val dao = db.propertyDao()

    val entity = PropertyEntity(
      id = "test_1",
      title = "Test Villa",
      address = "123 Ocean Drive",
      cityStateZip = "Miami, FL 33139",
      price = 1500000L,
      priceFormatted = "$1,500,000",
      beds = 3.0,
      baths = 2.0,
      sqft = 1800,
      propertyType = "House",
      description = "Lovely seaside villa",
      imageResId = 0,
      availableDates = listOf("Mon, Oct 14"),
      availableTimeSlots = listOf("10:00 AM"),
      amenities = listOf("Pool", "Garden")
    )
    dao.insertProperty(entity)
    val fetched = dao.findPropertyById("test_1")
    assertEquals("Test Villa", fetched?.title)
    assertEquals(1, dao.getCount())
    db.close()
  }

  @Test
  fun `estate view model instantiates via AndroidViewModelFactory`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(com.example.ui.viewmodel.EstateViewModel::class.java)
    org.junit.Assert.assertNotNull(viewModel)
  }

  @Test
  fun `agent dashboard metrics calculate 0 when empty and accurately update on completed viewings and inquiries`() {
    val appointments = listOf(
      com.example.data.model.ViewingAppointment(
        id = "appt_1",
        propertyId = "prop_1",
        propertyTitle = "Modern Villa",
        propertyAddress = "123 Ocean Blvd",
        date = "Oct 20",
        timeSlot = "2:00 PM",
        clientName = "Alice",
        clientPhone = "555-1234",
        status = com.example.data.model.AppointmentStatus.COMPLETED
      ),
      com.example.data.model.ViewingAppointment(
        id = "appt_2",
        propertyId = "prop_1",
        propertyTitle = "Modern Villa",
        propertyAddress = "123 Ocean Blvd",
        date = "Oct 21",
        timeSlot = "3:00 PM",
        clientName = "Bob",
        clientPhone = "555-5678",
        status = com.example.data.model.AppointmentStatus.UPCOMING
      )
    )

    val inquiries = listOf(
      com.example.data.model.Inquiry(
        id = "inq_1",
        propertyId = "prop_1",
        propertyTitle = "Modern Villa",
        propertyAddress = "123 Ocean Blvd",
        senderName = "Charlie",
        senderEmail = "charlie@test.com",
        senderPhone = "555-9999",
        message = "Is parking included?",
        timeAgo = "Just now",
        isUnread = true
      )
    )

    // When empty
    val emptyAppointments = emptyList<com.example.data.model.ViewingAppointment>()
    val emptyInquiries = emptyList<com.example.data.model.Inquiry>()
    assertEquals(0, emptyAppointments.count { it.status == com.example.data.model.AppointmentStatus.COMPLETED })
    assertEquals(0, emptyInquiries.count { it.isUnread || it.messages.none { m -> m.sender == "agent" } })

    // With actual data
    val completedCount = appointments.count { it.status == com.example.data.model.AppointmentStatus.COMPLETED }
    val upcomingCount = appointments.count { it.status == com.example.data.model.AppointmentStatus.UPCOMING || it.status == com.example.data.model.AppointmentStatus.PENDING }
    val pendingInquiries = inquiries.count { it.isUnread || it.messages.none { m -> m.sender == "agent" } }

    assertEquals(1, completedCount)
    assertEquals(1, upcomingCount)
    assertEquals(1, pendingInquiries)
  }

  @Test
  fun `property details back navigation returns to previous screen`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(com.example.ui.viewmodel.EstateViewModel::class.java)

    val sampleProp = com.example.data.model.Property(
      id = "test_prop_nav",
      title = "Test House",
      address = "456 Sunset Blvd",
      cityStateZip = "Miami, FL 33139",
      price = 1200000L,
      priceFormatted = "$1,200,000",
      beds = 3.0,
      baths = 2.0,
      sqft = 1500,
      propertyType = "House",
      description = "Cozy house"
    )

    // Flow 1: SEARCH -> PROPERTY_DETAILS -> close -> SEARCH
    viewModel.navigateTo(com.example.ui.viewmodel.Screen.SEARCH)
    assertEquals(com.example.ui.viewmodel.Screen.SEARCH, viewModel.uiState.value.currentScreen)
    viewModel.openPropertyDetails(sampleProp)
    assertEquals(com.example.ui.viewmodel.Screen.PROPERTY_DETAILS, viewModel.uiState.value.currentScreen)
    assertEquals(com.example.ui.viewmodel.Screen.SEARCH, viewModel.uiState.value.previousScreen)
    viewModel.closePropertyDetails()
    assertEquals(com.example.ui.viewmodel.Screen.SEARCH, viewModel.uiState.value.currentScreen)

    // Flow 2: SAVED -> PROPERTY_DETAILS -> close -> SAVED
    viewModel.navigateTo(com.example.ui.viewmodel.Screen.SAVED)
    viewModel.openPropertyDetails(sampleProp)
    assertEquals(com.example.ui.viewmodel.Screen.SAVED, viewModel.uiState.value.previousScreen)
    viewModel.closePropertyDetails()
    assertEquals(com.example.ui.viewmodel.Screen.SAVED, viewModel.uiState.value.currentScreen)

    // Flow 3: AGENT_DASHBOARD -> PROPERTY_DETAILS -> close -> AGENT_DASHBOARD
    viewModel.navigateTo(com.example.ui.viewmodel.Screen.AGENT_DASHBOARD)
    viewModel.openPropertyDetails(sampleProp)
    assertEquals(com.example.ui.viewmodel.Screen.AGENT_DASHBOARD, viewModel.uiState.value.previousScreen)
    viewModel.closePropertyDetails()
    assertEquals(com.example.ui.viewmodel.Screen.AGENT_DASHBOARD, viewModel.uiState.value.currentScreen)
  }

  @Test
  fun `inquiryDao markAsRead updates inquiry isUnread status in database`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, EstateDatabase::class.java).build()
    val dao = db.inquiryDao()

    val entity = com.example.data.local.entity.InquiryEntity(
      id = "inq_test_read",
      propertyId = "prop_1",
      propertyTitle = "Villa",
      propertyAddress = "123 Ocean Blvd",
      senderName = "Alice",
      senderEmail = "alice@example.com",
      senderPhone = "555-0101",
      message = "Hello",
      timeAgo = "1m ago",
      isUnread = true,
      messages = emptyList(),
      updatedAt = System.currentTimeMillis()
    )
    dao.insertInquiry(entity)

    val inserted = dao.getInquiryById("inq_test_read")
    org.junit.Assert.assertNotNull(inserted)
    org.junit.Assert.assertTrue(inserted!!.isUnread)

    dao.markAsRead("inq_test_read")

    val updated = dao.getInquiryById("inq_test_read")
    org.junit.Assert.assertNotNull(updated)
    org.junit.Assert.assertFalse(updated!!.isUnread)

    db.close()
  }

  @Test
  fun `backend connection info and DTO models initialize correctly`() {
    val backendInfo = com.example.data.remote.BackendConnectionInfo(
      state = com.example.data.remote.BackendConnectionState.CONNECTED,
      backendUrl = "http://10.0.2.2/estateflow-backend/api/",
      serverType = "PHP (XAMPP / Apache)",
      databaseEngine = "PostgreSQL",
      postgresVersion = "PostgreSQL 16.1",
      propertiesCount = 5
    )

    assertEquals(com.example.data.remote.BackendConnectionState.CONNECTED, backendInfo.state)
    assertEquals("PostgreSQL", backendInfo.databaseEngine)
    assertEquals(5, backendInfo.propertiesCount)

    val propDto = com.example.data.remote.PropertyDto(
      id = "test_prop_1",
      title = "Luxury Penthouse",
      address = "123 Main St",
      price = 2500000L,
      priceFormatted = "₱2,500,000",
      beds = 3.0,
      baths = 3.0,
      sqft = 2200
    )
    assertEquals("Luxury Penthouse", propDto.title)
    assertEquals(2500000L, propDto.price)
  }

  @Test
  fun `backend baseUrl is normalized for local and Wasmer deployments`() {
    val defaultUrl = com.example.data.remote.EstateApiClient.DEFAULT_EMULATOR_URL
    assertEquals("https://swu-005.wasmer.app/api/", defaultUrl)

    val normalized1 = com.example.data.remote.EstateApiClient.normalizeUrl("http://10.0.2.2")
    assertEquals("http://10.0.2.2/estateflow-backend/api/", normalized1)

    val normalized2 = com.example.data.remote.EstateApiClient.normalizeUrl("http://192.168.1.50/estateflow-backend")
    assertEquals("http://192.168.1.50/estateflow-backend/api/", normalized2)

    val normalized3 = com.example.data.remote.EstateApiClient.normalizeUrl("http://192.168.1.50/estateflow-backend/api/")
    assertEquals("http://192.168.1.50/estateflow-backend/api/", normalized3)

    val normalizedWasmer = com.example.data.remote.EstateApiClient.normalizeUrl(
      "https://swu-005.wasmer.app/estateflow-backend/api/"
    )
    assertEquals("https://swu-005.wasmer.app/api/", normalizedWasmer)

    val normalizedWasmerRoot = com.example.data.remote.EstateApiClient.normalizeUrl(
      "https://swu-005.wasmer.app"
    )
    assertEquals("https://swu-005.wasmer.app/api/", normalizedWasmerRoot)
  }

  @Test
  fun `deleting property removes record and cancels related appointments`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, EstateDatabase::class.java).build()
    val propDao = db.propertyDao()
    val apptDao = db.appointmentDao()

    val testProp = PropertyEntity(
      id = "prop_del_test",
      title = "Sunset Oasis",
      address = "900 Sunset Strip",
      cityStateZip = "Los Angeles, CA 90069",
      price = 4500000L,
      priceFormatted = "$4,500,000",
      beds = 5.0,
      baths = 4.0,
      sqft = 4200,
      propertyType = "House",
      description = "Stunning sunset views",
      imageResId = 0,
      availableDates = listOf("Fri, Oct 25"),
      availableTimeSlots = listOf("2:00 PM"),
      amenities = listOf("Pool", "Spa")
    )
    propDao.insertProperty(testProp)
    assertEquals(1, propDao.getCount())

    val testAppt = com.example.data.local.entity.ViewingAppointmentEntity(
      id = "appt_del_test",
      propertyId = "prop_del_test",
      propertyTitle = "Sunset Oasis",
      propertyAddress = "900 Sunset Strip",
      clientName = "Jane Doe",
      clientPhone = "555-0199",
      clientEmail = "jane@example.com",
      date = "Fri, Oct 25",
      timeSlot = "2:00 PM",
      status = com.example.data.model.AppointmentStatus.UPCOMING,
      notes = "Pre-screened buyer",
      timestamp = System.currentTimeMillis()
    )
    apptDao.insertAppointment(testAppt)

    // Execute deletion and cancellation
    propDao.deletePropertyById("prop_del_test")
    apptDao.cancelAppointmentsForProperty("prop_del_test", "Cancelled due to listing removal")

    assertEquals(0, propDao.getCount())
    val remainingAppts = apptDao.getAppointmentsByPropertyId("prop_del_test")
    assertEquals(1, remainingAppts.size)
    assertEquals(com.example.data.model.AppointmentStatus.CANCELLED, remainingAppts[0].status)
    org.junit.Assert.assertTrue(remainingAppts[0].notes.contains("Cancelled due to listing removal"))

    db.close()
  }

  @Test
  fun `non-agent buyer role cannot trigger property deletion`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    val viewModel = factory.create(com.example.ui.viewmodel.EstateViewModel::class.java)

    // Ensure role is BUYER
    if (viewModel.uiState.value.userRole == com.example.data.model.UserRole.AGENT) {
      viewModel.toggleUserRole()
    }
    assertEquals(com.example.data.model.UserRole.BUYER, viewModel.uiState.value.userRole)

    val sampleProp = com.example.data.model.Property(
      id = "protected_prop",
      title = "Protected Estate",
      address = "100 Safe Way",
      cityStateZip = "Miami, FL",
      price = 1000000L,
      priceFormatted = "$1,000,000",
      beds = 2.0,
      baths = 2.0,
      sqft = 1200,
      propertyType = "Condo",
      description = "Protected"
    )

    viewModel.openDeletePropertyDialog(sampleProp)
    // Deletion dialog must NOT open for BUYER
    org.junit.Assert.assertFalse(viewModel.uiState.value.isDeletePropertyDialogOpen)
  }

  @Test
  fun `repository deleteProperty returns PropertyDeleteResult`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, EstateDatabase::class.java).build()
    val repo = com.example.data.repository.EstateRepository.createForTesting(db, this)

    val testProp = PropertyEntity(
      id = "prop_repo_del",
      title = "Emerald Villa",
      address = "12 Ocean Dr",
      cityStateZip = "Honolulu, HI",
      price = 8500000L,
      priceFormatted = "$8,500,000",
      beds = 4.0,
      baths = 4.0,
      sqft = 3500,
      propertyType = "Villa",
      description = "Oceanfront",
      imageResId = 0,
      availableDates = listOf("Mon, Oct 14"),
      availableTimeSlots = listOf("10:00 AM"),
      amenities = listOf("Ocean View")
    )
    db.propertyDao().insertProperty(testProp)

    val result = repo.deleteProperty("prop_repo_del")
    org.junit.Assert.assertTrue(result.success)
    assertEquals("Emerald Villa", result.title)
    assertEquals(0, db.propertyDao().getCount())

    db.close()
  }
}
