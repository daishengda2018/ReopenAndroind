package com.dsd.baccarat.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dsd.baccarat.data.room.dao.BetDataDao
import com.dsd.baccarat.data.room.dao.GameSessionDao
import com.dsd.baccarat.data.room.dao.InputDataDao
import com.dsd.baccarat.data.room.dao.NoteDataDao
import com.dsd.baccarat.data.room.entity.BetEntity
import com.dsd.baccarat.data.room.entity.GameSessionEntity
import com.dsd.baccarat.data.room.entity.InputEntity
import com.dsd.baccarat.data.room.entity.NoteEntity

@Database(
    entities = [InputEntity::class, BetEntity::class, NoteEntity::class, GameSessionEntity::class],  // 正确：实体类列表
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    // 提供 DAO 实例（正确：返回 DAO 接口类型）
    abstract fun inputDataDao(): InputDataDao
    abstract fun betDataDao(): BetDataDao
    abstract fun noteDataDao(): NoteDataDao
    abstract fun gameSessionDao(): GameSessionDao

    // 单例模式实现（正确，线程安全）
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,  // 应用上下文，避免内存泄漏（正确）
                    AppDatabase::class.java,
                    "app_database"  // 建议：数据库名称更通用（如"app_database"），因为包含多个表
                )
                    .allowMainThreadQueries()  // 开发阶段临时使用，生产环境必须删除（正确）
                    // .addMigrations(MIGRATION_1_2)  // 版本升级时添加（当前v1无需）
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}