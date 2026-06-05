package com.gym.server.repository

import com.gym.shared.domain.GymUser
import com.gym.shared.domain.MemberProfile
import com.gym.shared.domain.UserRole
import com.gym.server.database.Users
import com.gym.server.database.MemberProfiles
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import com.gym.shared.domain.result.Result

class UserRepositoryImpl : UserRepository {
    override suspend fun findUserById(id: String): Result<GymUser?> = runCatchingTransaction {
        Users.selectAll().where { Users.id eq id }.singleOrNull()?.let { mapUser(it) }
    }

    override suspend fun findUserByEmail(email: String): Result<GymUser?> = runCatchingTransaction {
        Users.selectAll().where { Users.email eq email }.singleOrNull()?.let { mapUser(it) }
    }

    override suspend fun findAllUsers(): Result<List<GymUser>> = runCatchingTransaction {
        Users.selectAll().map { mapUser(it) }
    }

    override suspend fun findMemberProfile(userId: String): Result<MemberProfile?> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        MemberProfiles.selectAll().where { MemberProfiles.userId eq userId }.singleOrNull()?.let {
            MemberProfile(
                userId = it[MemberProfiles.userId],
                phone = it[MemberProfiles.phone],
                joinDate = it[MemberProfiles.joinDate].toInstant(tz),
                isActive = it[MemberProfiles.isActive],
                currentPlanId = it[MemberProfiles.currentPlanId]
            )
        }
    }

    override suspend fun findAllUsersWithProfiles(): Result<List<Triple<GymUser, MemberProfile, String?>>> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        (Users innerJoin MemberProfiles).selectAll().map { row ->
            val user = mapUser(row)
            val profile = MemberProfile(
                userId = row[MemberProfiles.userId],
                phone = row[MemberProfiles.phone],
                joinDate = row[MemberProfiles.joinDate].toInstant(tz),
                isActive = row[MemberProfiles.isActive],
                currentPlanId = row[MemberProfiles.currentPlanId]
            )
            Triple(user, profile, row[MemberProfiles.dni])
        }
    }

    override suspend fun findUserWithProfile(userId: String): Result<Triple<GymUser, MemberProfile, String?>?> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        (Users innerJoin MemberProfiles).selectAll().where { Users.id eq userId }.singleOrNull()?.let { row ->
            val user = mapUser(row)
            val profile = MemberProfile(
                userId = row[MemberProfiles.userId],
                phone = row[MemberProfiles.phone],
                joinDate = row[MemberProfiles.joinDate].toInstant(tz),
                isActive = row[MemberProfiles.isActive],
                currentPlanId = row[MemberProfiles.currentPlanId]
            )
            Triple(user, profile, row[MemberProfiles.dni])
        }
    }

    override suspend fun createUser(user: GymUser, passwordHash: String): Result<Unit> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        Users.insert {
            it[id] = user.id
            it[email] = user.email
            it[name] = user.name
            it[role] = user.role
            it[profileImageUrl] = user.profileImageUrl
            it[this.passwordHash] = passwordHash
            it[createdAt] = Clock.System.now().toLocalDateTime(tz)
        }
        Unit
    }

    override suspend fun createMemberProfile(profile: MemberProfile, dni: String?): Result<Unit> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        MemberProfiles.insert {
            it[userId] = profile.userId
            it[this.dni] = dni
            it[phone] = profile.phone
            it[joinDate] = profile.joinDate.toLocalDateTime(tz)
            it[isActive] = profile.isActive
            it[currentPlanId] = profile.currentPlanId
        }
        Unit
    }

    override suspend fun updateUser(id: String, email: String, name: String): Result<Unit> = runCatchingTransaction {
        Users.update({ Users.id eq id }) {
            it[this.email] = email
            it[this.name] = name
        }
        Unit
    }

    override suspend fun updateMemberProfile(userId: String, dni: String?, phone: String?, planId: String?): Result<Unit> = runCatchingTransaction {
        MemberProfiles.update({ MemberProfiles.userId eq userId }) {
            it[this.dni] = dni
            it[this.phone] = phone
            it[currentPlanId] = planId
        }
        Unit
    }

    override suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit> = runCatchingTransaction {
        Users.update({ Users.id eq userId }) {
            it[profileImageUrl] = imageUrl
        }
        Unit
    }

    override suspend fun validatePassword(email: String, passwordToCheck: String): Result<GymUser?> = runCatchingTransaction {
        Users.selectAll().where { (Users.email eq email) and (Users.passwordHash eq passwordToCheck) }.singleOrNull()?.let { mapUser(it) }
    }

    override suspend fun validateDni(email: String, dniToCheck: String): Result<GymUser?> = runCatchingTransaction {
        val userRow = Users.selectAll().where { Users.email eq email }.singleOrNull() ?: return@runCatchingTransaction null
        val profileRow = MemberProfiles.selectAll().where { MemberProfiles.userId eq userRow[Users.id] }.singleOrNull()
        if (profileRow != null && profileRow[MemberProfiles.dni] == dniToCheck) {
            mapUser(userRow)
        } else null
    }

    private fun mapUser(row: ResultRow): GymUser {
        return GymUser(
            id = row[Users.id],
            email = row[Users.email],
            name = row[Users.name],
            role = row[Users.role],
            profileImageUrl = row[Users.profileImageUrl]
        )
    }
}
