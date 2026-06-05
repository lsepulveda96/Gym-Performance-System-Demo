package com.gym.server.repository

import com.gym.shared.domain.GymPlan
import com.gym.shared.domain.result.Result

interface PlanRepository {
    suspend fun findAll(): Result<List<GymPlan>>
    suspend fun findById(id: String): Result<GymPlan?>
    suspend fun create(plan: GymPlan): Result<Unit>
    suspend fun update(id: String, plan: GymPlan): Result<Unit>
}
