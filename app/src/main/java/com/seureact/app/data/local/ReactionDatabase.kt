package com.seureact.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ProjectEntity::class, TimelineEventEntity::class], version = 1, exportSchema = false)
abstract class ReactionDatabase : RoomDatabase() {
    abstract fun reactionDao(): ReactionDao

    companion object {
        fun create(context: Context): ReactionDatabase = Room.databaseBuilder(
            context.applicationContext,
            ReactionDatabase::class.java,
            "reactions.db",
        ).build()
    }
}
