package com.gym.server.repository

import com.gym.shared.domain.CheckIn
import kotlinx.datetime.Instant
import com.gym.shared.domain.result.Result

interface CheckInRepository {
    suspend fun create(checkIn: CheckIn): Result<Unit>
    suspend fun getCheckInsByUserId(userId: String): Result<List<CheckIn>>
    suspend fun getWeeklyCheckInCount(userId: String, weekStart: Instant): Result<Int>
    suspend fun countCheckInsSince(timestamp: Instant): Result<Int>
    suspend fun getCheckInsSince(timestamp: Instant): Result<List<CheckIn>>
    suspend fun getRecentCheckIns(limit: Int): Result<List<CheckIn>>
}
