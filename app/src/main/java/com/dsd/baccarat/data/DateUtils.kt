package com.dsd.baccarat.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Create by Shengda 2025/11/4 19:17
 */


object DateUtils {

    /**
     * 将毫秒级时间戳转换为 LocalDate（本地时区）
     */
    fun timestampToLocalDate(timestamp: Long): LocalDate {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    /**
     * 从时间戳列表中提取去重的 LocalDate 列表（按时间升序排列）
     */
    fun extractUniqueDates(timestamps: List<Long>): List<LocalDate> {
        return timestamps
            .map { timestampToLocalDate(it) } // 转换为 LocalDate
            .distinct() // 去重
            .sorted() // 按日期升序排列（旧→新）
    }

    /**
     * 将“年/月/日”转换为当天00:00:00 ~ 23:59:59.999 的毫秒级时间戳（本地时区）
     */
    fun getDayStartAndEnd(date: LocalDate): Pair<Long, Long> {
        // 注意：LocalDate的月份是1-12，与Calendar的0-11不同
        val startTime = LocalDateTime.of(date, LocalTime.MIN).atZone(ZoneId.systemDefault()) // 本地时区 00:00:00
            .toInstant()
            .toEpochMilli()

        val endTime = LocalDateTime.of(date, LocalTime.MAX).atZone(ZoneId.systemDefault()) // 23:59:59.999
            .toInstant()
            .toEpochMilli()
        return Pair(startTime, endTime)
    }
}