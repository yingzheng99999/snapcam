package com.snapcam.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import com.snapcam.data.local.MediaDao
import com.snapcam.data.local.MediaDatabase
import com.snapcam.domain.repository.MediaRepository
import com.snapcam.data.repository.MediaRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MediaDatabase {
        return Room.databaseBuilder(
            context, MediaDatabase::class.java, "snapcam.db"
        ).build()
    }

    @Provides
    fun provideMediaDao(db: MediaDatabase): MediaDao = db.mediaDao()

    @Provides
    @Singleton
    fun provideMediaRepository(impl: MediaRepositoryImpl): MediaRepository = impl

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }
}
