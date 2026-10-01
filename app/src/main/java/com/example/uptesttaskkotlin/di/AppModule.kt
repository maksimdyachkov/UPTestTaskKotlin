package com.example.uptesttaskkotlin.di

import android.content.Context
import android.os.SystemClock
import androidx.room.Room
import com.example.uptesttaskkotlin.model.local.BarcodeDao
import com.example.uptesttaskkotlin.model.local.ScannerDatabase
import com.example.uptesttaskkotlin.model.repository.BarcodeRepository
import com.example.uptesttaskkotlin.model.repository.RoomBarcodeRepository
import com.example.uptesttaskkotlin.util.ElapsedClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindBarcodeRepository(repository: RoomBarcodeRepository): BarcodeRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): ScannerDatabase =
            Room.databaseBuilder(context, ScannerDatabase::class.java, ScannerDatabase.NAME).build()

        @Provides
        fun provideBarcodeDao(database: ScannerDatabase): BarcodeDao = database.barcodeDao()

        @Provides
        fun provideElapsedClock(): ElapsedClock = ElapsedClock(SystemClock::elapsedRealtime)
    }
}
