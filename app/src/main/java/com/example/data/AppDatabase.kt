package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "family_devices")
data class FamilyDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerName: String,
    val model: String,
    val phoneNumber: String = "+92-300-1234567",
    val imei: String = "357891043218765",
    val role: String = "MEMBER",
    val isOnline: Boolean = true,
    val batteryPct: Int = 85,
    val isArmed: Boolean = true,
    val isStolen: Boolean = false,
    val isLocked: Boolean = false,
    val isSirenActive: Boolean = false,
    val latitude: Double = 31.5204,
    val longitude: Double = 74.3587,
    val address: String = "Liberty Market, Lahore",
    val lastSeenTime: String = "Active now",
    val simNumber: String = "+92 300 ••••111",
    val emergencyPhone: String = "+92 300 4589211",
    val capturedPhotoCount: Int = 0,
    val lastPhotoUri: String? = null
)

@Entity(tableName = "intruder_captures")
data class IntruderCaptureEntity(
    @PrimaryKey val id: String,
    val timestamp: String,
    val triggerReason: String,
    val photoUri: String? = null,
    val wasPinWrong: Boolean = true
)

@Dao
interface FamilyDeviceDao {
    @Query("SELECT * FROM family_devices")
    fun getAllDevices(): Flow<List<FamilyDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: FamilyDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<FamilyDeviceEntity>)

    @Query("UPDATE family_devices SET phoneNumber = :phoneNumber WHERE id = :deviceId")
    suspend fun updatePhoneNumber(deviceId: String, phoneNumber: String)

    @Query("UPDATE family_devices SET isLocked = :isLocked WHERE id = :deviceId")
    suspend fun updateLockStatus(deviceId: String, isLocked: Boolean)

    @Query("UPDATE family_devices SET isSirenActive = :isActive WHERE id = :deviceId")
    suspend fun updateSirenStatus(deviceId: String, isActive: Boolean)

    @Query("UPDATE family_devices SET isStolen = :isStolen WHERE id = :deviceId")
    suspend fun updateStolenStatus(deviceId: String, isStolen: Boolean)

    @Query("DELETE FROM family_devices WHERE id = :deviceId")
    suspend fun deleteDevice(deviceId: String)
}

@Dao
interface IntruderCaptureDao {
    @Query("SELECT * FROM intruder_captures ORDER BY id DESC")
    fun getAllCaptures(): Flow<List<IntruderCaptureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: IntruderCaptureEntity)

    @Query("DELETE FROM intruder_captures WHERE id = :id")
    suspend fun deleteCapture(id: String)

    @Query("DELETE FROM intruder_captures")
    suspend fun clearAll()
}

@Entity(tableName = "location_history")
data class LocationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 10f,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String,
    val triggerSource: String = "PERIODIC_30S",
    val isSentViaSms: Boolean = false,
    val smsRecipient: String? = null,
    val isOffline: Boolean = false,
    val googleMapsUrl: String = "https://maps.google.com/?q=$latitude,$longitude"
)

@Dao
interface LocationHistoryDao {
    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    fun getAllLocations(): Flow<List<LocationHistoryEntity>>

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLocations(): Flow<List<LocationHistoryEntity>>

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT 1")
    fun getLastKnownLocation(): Flow<LocationHistoryEntity?>

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastKnownLocationOnce(): LocationHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationHistoryEntity): Long

    @Query("DELETE FROM location_history WHERE id = :id")
    suspend fun deleteLocation(id: Long)

    @Query("DELETE FROM location_history")
    suspend fun clearHistory()
}

@Database(
    entities = [FamilyDeviceEntity::class, IntruderCaptureEntity::class, LocationHistoryEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyDeviceDao(): FamilyDeviceDao
    abstract fun intruderCaptureDao(): IntruderCaptureDao
    abstract fun locationHistoryDao(): LocationHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thief_hunter_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
