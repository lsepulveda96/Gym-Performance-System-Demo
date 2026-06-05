package com.gym.server.service

import com.gym.server.repository.PlanRepository
import com.gym.shared.domain.GymPlan
import com.gym.shared.domain.result.Result
import java.util.UUID

class PlanService(private val planRepository: PlanRepository) {
    suspend fun getAllPlans(): Result<List<GymPlan>> {
        return planRepository.findAll()
    }
    
    suspend fun getPlanById(id: String): Result<GymPlan?> {
        return planRepository.findById(id)
    }
    
    suspend fun createPlan(request: GymPlan): Result<Unit> {
        val id = if (request.id.isEmpty()) "p-${UUID.randomUUID().toString().take(8)}" else request.id
        val newPlan = request.copy(id = id)
        return planRepository.create(newPlan)
    }
    
    suspend fun updatePlan(id: String, request: GymPlan): Result<Unit> {
        return planRepository.update(id, request)
    }
}
