package com.dsd.baccarat

import android.content.Context
import com.dsd.baccarat.data.Repository
import com.dsd.baccarat.data.room.AppDatabase
import com.dsd.baccarat.data.room.BetDataDao
import com.dsd.baccarat.data.room.InputDataDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideCountRepository(@ApplicationContext context: Context): Repository {
        return Repository(context)
    }

    // 关键：通过现有单例的 getInstance 方法提供 AppDatabase 实例
    @Provides
    @Singleton // 与单例逻辑匹配，确保全局唯一
    fun provideAppDatabase(
        @ApplicationContext context: Context // Hilt 自动注入应用上下文
    ): AppDatabase {
        // 直接调用你的单例方法，传入应用上下文
        return AppDatabase.getInstance(context)
    }

    // DAO 的提供方式不变（从单例数据库中获取）
    @Provides
    fun provideInputDataDao(database: AppDatabase): InputDataDao {
        return database.inputDataDao()
    }

    @Provides
    fun provideBetDataDao(database: AppDatabase): BetDataDao {
        return database.betDataDao()
    }
}