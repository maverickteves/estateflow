package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ViewingAppointmentEntity
import com.example.data.model.AppointmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

  @Query("SELECT * FROM appointments ORDER BY timestamp DESC")
  fun getAllAppointments(): Flow<List<ViewingAppointmentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAppointments(appointments: List<ViewingAppointmentEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAppointment(appointment: ViewingAppointmentEntity)

  @Query("UPDATE appointments SET status = :status WHERE id = :id")
  suspend fun updateStatus(id: String, status: AppointmentStatus)

  @Query("SELECT * FROM appointments WHERE propertyId = :propertyId")
  suspend fun getAppointmentsByPropertyId(propertyId: String): List<ViewingAppointmentEntity>

  @Query("UPDATE appointments SET status = 'CANCELLED', notes = :notes WHERE propertyId = :propertyId AND status != 'CANCELLED'")
  suspend fun cancelAppointmentsForProperty(
    propertyId: String,
    notes: String = "Viewing cancelled: Property listing was removed by the agent."
  )

  @Query("SELECT COUNT(*) FROM appointments")
  suspend fun getCount(): Int

  @Query("DELETE FROM appointments WHERE id NOT IN (:validIds)")
  suspend fun deleteAppointmentsNotIn(validIds: List<String>)

  @Query("DELETE FROM appointments")
  suspend fun clearAll()
}
