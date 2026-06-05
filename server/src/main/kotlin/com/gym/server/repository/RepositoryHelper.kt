package com.gym.server.repository

import com.gym.shared.domain.result.Result
import org.jetbrains.exposed.sql.transactions.transaction

inline fun <T> runCatchingDb(crossinline block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Throwable) {
        Result.Error(e.message ?: "Database operation failed", e)
    }
}

inline fun <T> runCatchingTransaction(crossinline block: () -> T): Result<T> {
    return try {
        transaction {
            Result.Success(block())
        }
    } catch (e: Throwable) {
        Result.Error(e.message ?: "Database transaction failed", e)
    }
}
