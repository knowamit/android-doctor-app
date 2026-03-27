package com.androidoctor.di

import android.content.Context
import androidx.room.Room
import com.androidoctor.data.local.AppDatabase
import com.androidoctor.data.local.dao.BenchmarkDao
import com.androidoctor.data.repository.DiagnosisRepositoryImpl
import com.androidoctor.data.repository.FixRepositoryImpl
import com.androidoctor.data.shell.ShellExecutor
import com.androidoctor.domain.repository.DiagnosisRepository
import com.androidoctor.domain.repository.FixRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDiagnosisRepository(impl: DiagnosisRepositoryImpl): DiagnosisRepository

    @Binds
    @Singleton
    abstract fun bindFixRepository(impl: FixRepositoryImpl): FixRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "android_doctor.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBenchmarkDao(db: AppDatabase): BenchmarkDao = db.benchmarkDao()
}
