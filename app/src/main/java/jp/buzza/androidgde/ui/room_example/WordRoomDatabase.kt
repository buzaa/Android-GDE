package jp.buzza.androidgde.ui.room_example

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Word::class],
    version = 1,
    exportSchema = false
)
abstract class WordRoomDatabase : RoomDatabase() {
    companion object {
        fun getDatabase(context: Context): WordRoomDatabase {
            return Room.inMemoryDatabaseBuilder(context, WordRoomDatabase::class.java)
                .fallbackToDestructiveMigration()
                .build()
        }
    }

    abstract fun wordDao(): WordDao

}
