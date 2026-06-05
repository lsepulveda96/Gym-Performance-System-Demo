package com.gym.server.fake

import com.gym.server.repository.CheckInRepository
import com.gym.shared.domain.CheckIn
import com.gym.shared.domain.result.Result
import kotlinx.datetime.Instant

/**
 * In-memory [CheckInRepository] for access and attendance tests.
 */
class FakeCheckInRepository : CheckInRepository {
    val checkIns = mutableListOf<CheckIn>()
    var weeklyCountOverride: Int? = null

    fun seed(checkIn: CheckIn) {
        checkIns.add(checkIn)
    }

    override suspend fun create(checkIn: CheckIn): Result<Unit> {
        checkIns.add(checkIn)
        return Result.Success(Unit)
    }

    override suspend fun getCheckInsByUserId(userId: String): Result<List<CheckIn>> =
        Result.Success(checkIns.filter { it.userId == userId })

    override suspend fun getWeeklyCheckInCount(userId: String, weekStart: Instant): Result<Int> {
        weeklyCountOverride?.let { return Result.Success(it) }
        val count = checkIns.count { it.userId == userId && it.timestamp >= weekStart }
        return Result.Success(count)
    }

    override suspend fun countCheckInsSince(timestamp: Instant): Result<Int> =
        Result.Success(checkIns.count { it.timestamp >= timestamp })

    override suspend fun getCheckInsSince(timestamp: Instant): Result<List<CheckIn>> =
        Result.Success(checkIns.filter { it.timestamp >= timestamp })

    override suspend fun getRecentCheckIns(limit: Int): Result<List<CheckIn>> =
        Result.Success(checkIns.sortedByDescending { it.timestamp }.take(limit))
}
