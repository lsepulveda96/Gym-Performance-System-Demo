package com.gym.server.fake

import com.gym.server.repository.PlanRepository
import com.gym.shared.domain.GymPlan
import com.gym.shared.domain.result.Result

/**
 * In-memory [PlanRepository] for deterministic plan lookups in service tests.
 */
class FakePlanRepository : PlanRepository {
    private val plans = mutableMapOf<String, GymPlan>()

    fun seed(plan: GymPlan) {
        plans[plan.id] = plan
    }

    fun clear() = plans.clear()

    override suspend fun findAll(): Result<List<GymPlan>> =
        Result.Success(plans.values.toList())

    override suspend fun findById(id: String): Result<GymPlan?> =
        Result.Success(plans[id])

    override suspend fun create(plan: GymPlan): Result<Unit> {
        plans[plan.id] = plan
        return Result.Success(Unit)
    }

    override suspend fun update(id: String, plan: GymPlan): Result<Unit> {
        plans[id] = plan
        return Result.Success(Unit)
    }
}
