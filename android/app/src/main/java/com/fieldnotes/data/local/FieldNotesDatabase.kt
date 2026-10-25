package com.fieldnotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ItemEntity::class,
        TemplateEntity::class,
        TemplateItemEntity::class,
        PlanEntity::class,
        PlanItemEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class FieldNotesDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun templateDao(): TemplateDao
    abstract fun planDao(): PlanDao

    companion object {
        fun build(context: Context): FieldNotesDatabase =
            Room.databaseBuilder(context, FieldNotesDatabase::class.java, "fieldnotes.db")
                .build()
    }
}
