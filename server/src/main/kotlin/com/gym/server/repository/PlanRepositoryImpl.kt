package com.gym.server.repository

import com.gym.shared.domain.GymPlan
import com.gym.server.database.Plans
import com.gym.shared.domain.result.Result
import org.jetbrains.exposed.sql.*

class PlanRepositoryImpl : PlanRepository {
    override suspend fun findAll(): Result<List<GymPlan>> = runCatchingTransaction {
        Plans.selectAll().map {
            GymPlan(
                id = it[Plans.id],
                name = it[Plans.name],
                price = it[Plans.price],
                durationDays = it[Plans.durationDays],
                description = it[Plans.description],
                weeklyLimit = it[Plans.weeklyLimit]
            )
        }
    }

    override suspend fun findById(id: String): Result<GymPlan?> = runCatchingTransaction {
        Plans.selectAll().where { Plans.id eq id }.singleOrNull()?.let {
            GymPlan(
                id = it[Plans.id],
                name = it[Plans.name],
                price = it[Plans.price],
                durationDays = it[Plans.durationDays],
                description = it[Plans.description],
                weeklyLimit = it[Plans.weeklyLimit]
            )
        }
    }

    override suspend fun create(plan: GymPlan): Result<Unit> = runCatchingTransaction {
        Plans.insert {
            it[id] = plan.id
            it[name] = plan.name
            it[price] = plan.price
            it[durationDays] = plan.durationDays
            it[description] = plan.description
            it[weeklyLimit] = plan.weeklyLimit
        }
        Unit
    }

    override suspend fun update(id: String, plan: GymPlan): Result<Unit> = runCatchingTransaction {
        Plans.update({ Plans.id eq id }) {
            it[name] = plan.name
            it[price] = plan.price
            it[durationDays] = plan.durationDays
            it[description] = plan.description
            it[weeklyLimit] = plan.weeklyLimit
        }
        Unit
    }
}
