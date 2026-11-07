package com.seureact.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProjectEntity::class, TimelineEventEntity::class], version = 2, exportSchema = false)
abstract class ReactionDatabase : RoomDatabase() {
    abstract fun reactionDao(): ReactionDao

    companion object {
        fun create(context: Context): ReactionDatabase = Room.databaseBuilder(
            context.applicationContext,
            ReactionDatabase::class.java,
            "reactions.db",
        ).addMigrations(MIGRATION_1_2).build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timeline_events ADD COLUMN stringValue TEXT")
            }
        }
    }
}
