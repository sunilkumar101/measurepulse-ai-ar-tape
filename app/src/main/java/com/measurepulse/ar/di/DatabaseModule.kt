package com.measurepulse.ar.di

import android.content.Context
import androidx.room.Room
import com.measurepulse.ar.data.local.MeasurePulseDatabase
import com.measurepulse.ar.data.local.RoomScanDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MeasurePulseDatabase =
        Room.databaseBuilder(context, MeasurePulseDatabase::class.java, MeasurePulseDatabase.NAME).build()

    @Provides
    fun provideRoomScanDao(db: MeasurePulseDatabase): RoomScanDao = db.roomScanDao()
}
