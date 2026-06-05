package com.gym.server.fake

import com.gym.server.repository.UserRepository
import com.gym.shared.domain.GymUser
import com.gym.shared.domain.MemberProfile
import com.gym.shared.domain.result.Result

/**
 * In-memory [UserRepository] for member-related service tests.
 * No database access; state is fully controlled in tests.
 */
class FakeMemberRepository : UserRepository {
    val users = mutableMapOf<String, GymUser>()
    val profiles = mutableMapOf<String, MemberProfile>()
    private val dniByUserId = mutableMapOf<String, String?>()
    private val dniIndex = mutableMapOf<String, String>()

    fun seedMember(user: GymUser, profile: MemberProfile, dni: String? = "12345678") {
        users[user.id] = user
        profiles[user.id] = profile
        dniByUserId[user.id] = dni
        if (dni != null) dniIndex[dni] = user.id
    }

    fun registeredEmails(): Set<String> = users.values.map { it.email }.toSet()

    override suspend fun findUserById(id: String): Result<GymUser?> =
        Result.Success(users[id])

    override suspend fun findUserByEmail(email: String): Result<GymUser?> =
        Result.Success(users.values.find { it.email.equals(email, ignoreCase = false) })

    override suspend fun findAllUsers(): Result<List<GymUser>> =
        Result.Success(users.values.toList())

    override suspend fun findMemberProfile(userId: String): Result<MemberProfile?> =
        Result.Success(profiles[userId])

    override suspend fun findAllUsersWithProfiles(): Result<List<Triple<GymUser, MemberProfile, String?>>> =
        Result.Success(
            users.values.mapNotNull { user ->
                profiles[user.id]?.let { profile -> Triple(user, profile, dniByUserId[user.id]) }
            },
        )

    override suspend fun findUserWithProfile(userId: String): Result<Triple<GymUser, MemberProfile, String?>?> {
        val user = users[userId] ?: return Result.Success(null)
        val profile = profiles[userId] ?: return Result.Success(null)
        return Result.Success(Triple(user, profile, dniByUserId[userId]))
    }

    override suspend fun createUser(user: GymUser, passwordHash: String): Result<Unit> {
        users[user.id] = user
        return Result.Success(Unit)
    }

    override suspend fun createMemberProfile(profile: MemberProfile, dni: String?): Result<Unit> {
        if (dni != null && dniIndex.containsKey(dni)) {
            return Result.Error(
                message = "member_profiles_dni_unique constraint violated",
                cause = IllegalStateException("member_profiles_dni_unique"),
            )
        }
        profiles[profile.userId] = profile
        dniByUserId[profile.userId] = dni
        if (dni != null) dniIndex[dni] = profile.userId
        return Result.Success(Unit)
    }

    override suspend fun updateUser(id: String, email: String, name: String): Result<Unit> {
        val user = users[id] ?: return Result.Error("User not found")
        users[id] = user.copy(email = email, name = name)
        return Result.Success(Unit)
    }

    override suspend fun updateMemberProfile(
        userId: String,
        dni: String?,
        phone: String?,
        planId: String?,
    ): Result<Unit> {
        val profile = profiles[userId] ?: return Result.Error("Profile not found")
        if (dni != null && dniIndex[dni] != null && dniIndex[dni] != userId) {
            return Result.Error(
                message = "member_profiles_dni_unique constraint violated",
                cause = IllegalStateException("member_profiles_dni_unique"),
            )
        }
        dniByUserId[userId]?.let { old -> dniIndex.remove(old) }
        profiles[userId] = profile.copy(phone = phone, currentPlanId = planId)
        dniByUserId[userId] = dni
        if (dni != null) dniIndex[dni] = userId
        return Result.Success(Unit)
    }

    override suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit> {
        val user = users[userId] ?: return Result.Error("User not found")
        users[userId] = user.copy(profileImageUrl = imageUrl)
        return Result.Success(Unit)
    }

    override suspend fun validatePassword(email: String, passwordToCheck: String): Result<GymUser?> =
        Result.Success(null)

    override suspend fun validateDni(email: String, dniToCheck: String): Result<GymUser?> =
        Result.Success(null)
}
