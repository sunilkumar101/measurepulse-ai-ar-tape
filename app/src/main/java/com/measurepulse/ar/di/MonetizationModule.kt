package com.measurepulse.ar.di

import com.measurepulse.ar.data.monetization.ExportGate
import com.measurepulse.ar.data.monetization.RewardedExportGate
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MonetizationModule {
    @Binds
    abstract fun bindExportGate(impl: RewardedExportGate): ExportGate
}
