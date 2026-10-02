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

    @Query("DELETE FROM location_history WHERE timestamp < :timestamp")
    suspend fun deleteLocationsOlderThan(timestamp: Long)

    @Query("DELETE FROM location_history")
    suspend fun clearHistory()
}

@Entity(tableName = "owner_face_samples")
data class OwnerFaceSampleEntity(
    @PrimaryKey val id: String,
    val sampleIndex: Int,
    val photoUri: String,
    val timestamp: Long = System.currentTimeMillis(),
    val featureMetrics: String = ""
)

@Dao
interface OwnerFaceDao {
    @Query("SELECT * FROM owner_face_samples ORDER BY sampleIndex ASC")
    fun getAllSamples(): Flow<List<OwnerFaceSampleEntity>>

    @Query("SELECT COUNT(*) FROM owner_face_samples")
    suspend fun getSampleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<OwnerFaceSampleEntity>)

    @Query("DELETE FROM owner_face_samples")
    suspend fun clearAll()
}

@Entity(tableName = "user_devices")
data class UserDeviceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val model: String,
    val imei: String,
    val batteryPercent: Int = 95,
    val isSecured: Boolean = true,
    val isLocked: Boolean = false,
    val isSirenPlaying: Boolean = false,
    val simCardNumber: String = "+92 300 ••••123",
    val lastSeenAddress: String = "Current Location",
    val lastSeenTime: String = "Just now",
    val isStolen: Boolean = false,
    val stolenTimestamp: String? = null,
    val emergencyContactPhone: String = "+92 300 4589211",
    val lastKnownLatitude: Double = 31.5204,
    val lastKnownLongitude: Double = 74.3587
)

@Dao
interface UserDeviceDao {
    @Query("SELECT * FROM user_devices ORDER BY id ASC")
    fun getAllDevices(): Flow<List<UserDeviceEntity>>

    @Query("SELECT * FROM user_devices ORDER BY id ASC")
    suspend fun getAllDevicesOnce(): List<UserDeviceEntity>

    @Query("SELECT * FROM user_devices WHERE id = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): UserDeviceEntity?

    @Query("SELECT * FROM user_devices WHERE imei = :imei LIMIT 1")
    suspend fun getDeviceByImei(imei: String): UserDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: UserDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<UserDeviceEntity>)

    @Query("UPDATE user_devices SET imei = :imei WHERE id = :deviceId")
    suspend fun updateImei(deviceId: String, imei: String)

    @Query("UPDATE user_devices SET emergencyContactPhone = :phone WHERE id = :deviceId")
    suspend fun updateEmergencyPhone(deviceId: String, phone: String)

    @Query("UPDATE user_devices SET isLocked = :isLocked, isSecured = :isSecured WHERE id = :deviceId")
    suspend fun updateLockStatus(deviceId: String, isLocked: Boolean, isSecured: Boolean)

    @Query("UPDATE user_devices SET isSirenPlaying = :isPlaying WHERE id = :deviceId")
    suspend fun updateSirenStatus(deviceId: String, isPlaying: Boolean)

    @Query("DELETE FROM user_devices WHERE id = :deviceId")
    suspend fun deleteDevice(deviceId: String)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY id DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Query("SELECT * FROM devices ORDER BY id DESC")
    suspend fun getAllDevicesOnce(): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: Long): DeviceEntity?

    @Query("SELECT * FROM devices WHERE imei = :imei LIMIT 1")
    suspend fun getDeviceByImei(imei: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceEntity): Long

    @Update
    suspend fun updateDevice(device: DeviceEntity)

    @Query("UPDATE devices SET isStolen = :isStolen WHERE id = :id")
    suspend fun updateStolenStatus(id: Long, isStolen: Boolean)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDevice(id: Long)

    @Query("DELETE FROM devices")
    suspend fun deleteAllDevices()
}

@Database(
    entities = [
        FamilyDeviceEntity::class,
        IntruderCaptureEntity::class,
        LocationHistoryEntity::class,
        OwnerFaceSampleEntity::class,
        UserDeviceEntity::class,
        DeviceEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyDeviceDao(): FamilyDeviceDao
    abstract fun intruderCaptureDao(): IntruderCaptureDao
    abstract fun locationHistoryDao(): LocationHistoryDao
    abstract fun ownerFaceDao(): OwnerFaceDao
    abstract fun userDeviceDao(): UserDeviceDao
    abstract fun deviceDao(): DeviceDao

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
