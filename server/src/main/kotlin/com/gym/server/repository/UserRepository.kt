package com.gym.server.repository

import com.gym.shared.domain.GymUser
import com.gym.shared.domain.MemberProfile
import com.gym.shared.domain.UserRole
import com.gym.shared.domain.result.Result

interface UserRepository {
    suspend fun findUserById(id: String): Result<GymUser?>
    suspend fun findUserByEmail(email: String): Result<GymUser?>
    suspend fun findAllUsers(): Result<List<GymUser>>
    
    suspend fun findMemberProfile(userId: String): Result<MemberProfile?>
    suspend fun findAllUsersWithProfiles(): Result<List<Triple<GymUser, MemberProfile, String?>>>
    suspend fun findUserWithProfile(userId: String): Result<Triple<GymUser, MemberProfile, String?>?>
    
    suspend fun createUser(user: GymUser, passwordHash: String): Result<Unit>
    suspend fun createMemberProfile(profile: MemberProfile, dni: String?): Result<Unit>
    
    suspend fun updateUser(id: String, email: String, name: String): Result<Unit>
    suspend fun updateMemberProfile(userId: String, dni: String?, phone: String?, planId: String?): Result<Unit>
    suspend fun updateProfileImage(userId: String, imageUrl: String): Result<Unit>
    
    suspend fun validatePassword(email: String, passwordToCheck: String): Result<GymUser?>
    suspend fun validateDni(email: String, dniToCheck: String): Result<GymUser?>
}
