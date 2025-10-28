package com.dsd.baccarat.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SerializationUtils {
    private val json = Json { ignoreUnknownKeys = true }

    fun serializeInputTypeList(list: List<InputType>): String {
        return json.encodeToString(list)
    }

    fun deserializeInputTypeList(jsonString: String): List<InputType> {
        return json.decodeFromString(jsonString)
    }

    fun serializeBeltResultTypeList(list: List<BeltResultType>): String {
        return json.encodeToString(list)
    }

    fun deserializeBeltResultTypeList(jsonString: String): List<BeltResultType> {
        return json.decodeFromString(jsonString)
    }
}