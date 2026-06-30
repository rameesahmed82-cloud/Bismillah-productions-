package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    // User Ratings queries
    @Query("SELECT * FROM user_ratings ORDER BY timestamp DESC")
    fun getAllUserRatings(): Flow<List<UserRating>>

    @Query("SELECT * FROM user_ratings WHERE placeId = :placeId ORDER BY timestamp DESC")
    fun getRatingsForPlace(placeId: String): Flow<List<UserRating>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserRating(rating: UserRating)

    // Bookmarks queries
    @Query("SELECT * FROM bookmarked_places ORDER BY savedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkedPlace>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_places WHERE placeId = :placeId)")
    fun isBookmarked(placeId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedPlace)

    @Query("DELETE FROM bookmarked_places WHERE placeId = :placeId")
    suspend fun deleteBookmark(placeId: String)
}

@Database(entities = [UserRating::class, BookmarkedPlace::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_nearby_nav_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
