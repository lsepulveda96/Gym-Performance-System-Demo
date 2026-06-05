package com.gym.server.repository

import com.gym.shared.domain.Payment
import com.gym.server.database.Payments
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import com.gym.shared.domain.result.Result

class PaymentRepositoryImpl : PaymentRepository {
    override suspend fun create(payment: Payment): Result<Unit> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        Payments.insert {
            it[id] = payment.id
            it[userId] = payment.userId
            it[amount] = payment.amount
            it[paymentDate] = payment.paymentDate.toLocalDateTime(tz)
            it[expirationDate] = payment.expirationDate.toLocalDateTime(tz)
            it[method] = payment.method
            it[timestamp] = payment.paymentDate.toLocalDateTime(tz)
        }
        Unit
    }

    override suspend fun findByUserIdOrderByDateDesc(userId: String): Result<List<Payment>> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        Payments.selectAll().where { Payments.userId eq userId }
            .orderBy(Payments.paymentDate, SortOrder.DESC)
            .map {
                Payment(
                    id = it[Payments.id],
                    userId = it[Payments.userId],
                    amount = it[Payments.amount],
                    paymentDate = it[Payments.paymentDate].toInstant(tz),
                    expirationDate = it[Payments.expirationDate].toInstant(tz),
                    method = it[Payments.method]
                )
            }
    }

    override suspend fun findLatestPaymentForUser(userId: String): Result<Payment?> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        Payments.selectAll().where { Payments.userId eq userId }
            .orderBy(Payments.expirationDate, SortOrder.DESC)
            .limit(1)
            .singleOrNull()?.let {
                Payment(
                    id = it[Payments.id],
                    userId = it[Payments.userId],
                    amount = it[Payments.amount],
                    paymentDate = it[Payments.paymentDate].toInstant(tz),
                    expirationDate = it[Payments.expirationDate].toInstant(tz),
                    method = it[Payments.method]
                )
            }
    }

    override suspend fun findLatestPaymentsForAllUsers(): Result<Map<String, Payment>> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        val allPayments = Payments.selectAll().orderBy(Payments.expirationDate, SortOrder.DESC).map {
            Payment(
                id = it[Payments.id],
                userId = it[Payments.userId],
                amount = it[Payments.amount],
                paymentDate = it[Payments.paymentDate].toInstant(tz),
                expirationDate = it[Payments.expirationDate].toInstant(tz),
                method = it[Payments.method]
            )
        }
        val result = mutableMapOf<String, Payment>()
        for (p in allPayments) {
            if (!result.containsKey(p.userId)) {
                result[p.userId] = p
            }
        }
        result
    }

    override suspend fun getPaymentsInDateRange(startDate: Instant, endDate: Instant): Result<List<Payment>> = runCatchingTransaction {
        val tz = TimeZone.currentSystemDefault()
        val startLocal = startDate.toLocalDateTime(tz)
        val endLocal = endDate.toLocalDateTime(tz)
        
        Payments.selectAll().where { 
            (Payments.paymentDate greaterEq startLocal) and (Payments.paymentDate less endLocal)
        }.map {
            Payment(
                id = it[Payments.id],
                userId = it[Payments.userId],
                amount = it[Payments.amount],
                paymentDate = it[Payments.paymentDate].toInstant(tz),
                expirationDate = it[Payments.expirationDate].toInstant(tz),
                method = it[Payments.method]
            )
        }
    }
}
