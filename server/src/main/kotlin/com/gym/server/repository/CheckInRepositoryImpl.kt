package com.gym.server.repository

import com.gym.shared.domain.CheckIn
import com.gym.server.database.CheckIns
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import com.gym.shared.domain.result.Result

class CheckInRepositoryImpl : CheckInRepository {
    override suspend fun create(checkIn: CheckIn): Result<Unit> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        CheckIns.insert {
            it[id] = checkIn.id
            it[userId] = checkIn.userId
            it[timestamp] = checkIn.timestamp.toLocalDateTime(tz)
            it[planIdAtTime] = checkIn.planIdAtTime
        }
        Unit
    }

    override suspend fun getCheckInsByUserId(userId: String): Result<List<CheckIn>> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        CheckIns.selectAll().where { CheckIns.userId eq userId }
            .orderBy(CheckIns.timestamp, SortOrder.DESC)
            .map {
                CheckIn(
                    id = it[CheckIns.id],
                    userId = it[CheckIns.userId],
                    timestamp = it[CheckIns.timestamp].toInstant(tz),
                    planIdAtTime = it[CheckIns.planIdAtTime]
                )
            }
    }

    override suspend fun getWeeklyCheckInCount(userId: String, weekStart: Instant): Result<Int> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        val startLocal = weekStart.toLocalDateTime(tz)
        CheckIns.selectAll().where {
            (CheckIns.userId eq userId) and (CheckIns.timestamp greaterEq startLocal)
        }.count().toInt()
    }

    override suspend fun countCheckInsSince(timestamp: Instant): Result<Int> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        val startLocal = timestamp.toLocalDateTime(tz)
        CheckIns.selectAll().where {
            CheckIns.timestamp greaterEq startLocal
        }.count().toInt()
    }

    override suspend fun getCheckInsSince(timestamp: Instant): Result<List<CheckIn>> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        val startLocal = timestamp.toLocalDateTime(tz)
        CheckIns.selectAll().where {
            CheckIns.timestamp greaterEq startLocal
        }.map {
            CheckIn(
                id = it[CheckIns.id],
                userId = it[CheckIns.userId],
                timestamp = it[CheckIns.timestamp].toInstant(tz),
                planIdAtTime = it[CheckIns.planIdAtTime]
            )
        }
    }

    override suspend fun getRecentCheckIns(limit: Int): Result<List<CheckIn>> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        CheckIns.selectAll()
            .orderBy(CheckIns.timestamp, SortOrder.DESC)
            .limit(limit)
            .map {
                CheckIn(
                    id = it[CheckIns.id],
                    userId = it[CheckIns.userId],
                    timestamp = it[CheckIns.timestamp].toInstant(tz),
                    planIdAtTime = it[CheckIns.planIdAtTime]
                )
            }
    }
}
