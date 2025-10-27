package com.dsd.baccarat

import android.content.Context
import com.dsd.baccarat.data.CountRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideCountRepository(@ApplicationContext context: Context): CountRepository {
        return CountRepository(context)
    }
}