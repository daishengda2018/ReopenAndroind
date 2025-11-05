package com.dsd.baccarat.data.utils

import com.dsd.baccarat.data.room.entity.BetEntity
import com.dsd.baccarat.data.room.entity.InputEntity
import kotlinx.serialization.json.Json

object SerializationUtils {
    // 配置 Json 实例（按需调整规则）
    val json = Json {
        ignoreUnknownKeys = true // 反序列化时忽略未知字段（避免新增字段导致崩溃）
        isLenient = true // 允许宽松的 JSON 格式（如单引号、尾逗号）
        encodeDefaults = true // 序列化时包含默认值（如 curTime = 0 会被序列化）
    }

    fun serializeInputTypeList(list: List<InputEntity>): String {
        return json.encodeToString(list)
    }

    fun deserializeInputDataList(jsonString: String): List<InputEntity> {
        return json.decodeFromString(jsonString)
    }

    fun serializeBeltResultTypeList(list: List<BetEntity>): String {
        return json.encodeToString(list)
    }

    fun deserializeBeltResultTypeList(jsonString: String): List<BetEntity> {
        return json.decodeFromString(jsonString)
    }
}