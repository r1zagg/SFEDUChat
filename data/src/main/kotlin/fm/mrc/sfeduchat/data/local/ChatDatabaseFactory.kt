package fm.mrc.sfeduchat.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ChatDatabaseFactory {
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE chats ADD COLUMN unreadCount INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            println("DEBUG MIGRATION_5_6: Starting migration")
            try {
                database.execSQL("ALTER TABLE chats ADD COLUMN unreadCount INTEGER NOT NULL DEFAULT 0")
                println("DEBUG MIGRATION_5_6: Successfully added unreadCount column")
            } catch (e: Exception) {
                println("DEBUG MIGRATION_5_6: Error adding column - ${e.message}")
            }
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            println("DEBUG MIGRATION_6_7: Starting migration")
            try {
                database.execSQL("ALTER TABLE chats ADD COLUMN unreadCount INTEGER NOT NULL DEFAULT 0")
                println("DEBUG MIGRATION_6_7: Successfully added unreadCount column")
            } catch (e: Exception) {
                println("DEBUG MIGRATION_6_7: Error adding column - ${e.message}")
            }
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            println("DEBUG MIGRATION_7_8: Starting migration - forcing destructive recreation")
            // This will trigger destructive migration
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            println("DEBUG MIGRATION_8_9: Starting migration - forcing destructive recreation to remove unreadCount")
            // This will trigger destructive migration
        }
    }

    fun create(context: Context): ChatDatabase =
        Room.databaseBuilder(context, ChatDatabase::class.java, "sfedu_chat.db")
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
}
