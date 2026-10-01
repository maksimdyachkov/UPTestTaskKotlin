package com.example.uptesttaskkotlin.di

import android.os.SystemClock
import com.example.uptesttaskkotlin.model.repository.BarcodeRepository
import com.example.uptesttaskkotlin.model.repository.InMemoryBarcodeRepository
import com.example.uptesttaskkotlin.util.ElapsedClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindBarcodeRepository(repository: InMemoryBarcodeRepository): BarcodeRepository

    companion object {
        @Provides
        fun provideElapsedClock(): ElapsedClock = ElapsedClock(SystemClock::elapsedRealtime)
    }
}
