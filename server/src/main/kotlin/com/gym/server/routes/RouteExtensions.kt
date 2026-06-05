package com.gym.server.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.HttpStatusCode
import com.gym.shared.domain.result.Result

suspend inline fun <reified T : Any, reified R : Any> ApplicationCall.respondResult(
    result: Result<T>,
    crossinline mapper: (T) -> R
) {
    when (result) {
        is Result.Success -> {
            val mapped = mapper(result.data)
            respond(mapped)
        }
        is Result.Error -> {
            respond(HttpStatusCode.InternalServerError, mapOf("error" to result.message))
        }
    }
}

suspend inline fun <reified T : Any> ApplicationCall.respondResult(
    result: Result<T>
) {
    when (result) {
        is Result.Success -> respond(result.data)
        is Result.Error -> respond(HttpStatusCode.InternalServerError, mapOf("error" to result.message))
    }
}

suspend inline fun <reified T : Any, reified R : Any> ApplicationCall.respondResultWithStatus(
    result: Result<T>,
    status: HttpStatusCode,
    crossinline mapper: (T) -> R
) {
    when (result) {
        is Result.Success -> {
            val mapped = mapper(result.data)
            respond(status, mapped)
        }
        is Result.Error -> {
            respond(HttpStatusCode.InternalServerError, mapOf("error" to result.message))
        }
    }
}

suspend inline fun ApplicationCall.respondResultWithStatus(
    result: Result<Unit>,
    status: HttpStatusCode
) {
    when (result) {
        is Result.Success -> respond(status)
        is Result.Error -> respond(HttpStatusCode.InternalServerError, mapOf("error" to result.message))
    }
}
