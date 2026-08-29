package com.androidoctor.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.analytics.FirebaseAnalytics
import com.androidoctor.data.local.AppDatabase
import com.androidoctor.data.local.dao.BenchmarkDao
import com.androidoctor.data.local.dao.FixChangeDao
import com.androidoctor.data.local.dao.FixSessionDao
import com.androidoctor.data.local.dao.ScoreSnapshotDao
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
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideBenchmarkDao(db: AppDatabase): BenchmarkDao = db.benchmarkDao()

    @Provides
    fun provideScoreSnapshotDao(db: AppDatabase): ScoreSnapshotDao = db.scoreSnapshotDao()

    @Provides
    fun provideFixSessionDao(db: AppDatabase): FixSessionDao = db.fixSessionDao()

    @Provides
    fun provideFixChangeDao(db: AppDatabase): FixChangeDao = db.fixChangeDao()
}

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)
}
