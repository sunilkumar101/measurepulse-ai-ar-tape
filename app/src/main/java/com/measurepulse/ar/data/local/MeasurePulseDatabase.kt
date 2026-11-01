package com.measurepulse.ar.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter fun fromCategory(c: ScanCategory): String = c.name
    @TypeConverter fun toCategory(s: String): ScanCategory = ScanCategory.valueOf(s)
}

@Database(
    entities = [RoomScanEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // v2: polygonJson
        AutoMigration(from = 2, to = 3), // v3: openingsJson
    ],
)
@TypeConverters(Converters::class)
abstract class MeasurePulseDatabase : RoomDatabase() {
    abstract fun roomScanDao(): RoomScanDao

    companion object {
        const val NAME = "measurepulse.db"
    }
}
