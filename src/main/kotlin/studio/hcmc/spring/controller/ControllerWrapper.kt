@file:Suppress("DuplicatedCode")

package studio.hcmc.spring.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jdk.jfr.ContentType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import studio.hcmc.kotlin.protocol.io.ErrorDataTransferObject
import studio.hcmc.kotlin.protocol.io.Response
import kotlin.coroutines.CoroutineContext

interface ControllerWrapper {
    companion object {
        var defaultJson: Json = Json
    }

    fun Throwable.httpStatueCode(): Int {
        if (this is ErrorDataTransferObject) {
            return httpStatusCode
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR.value()
        }
    }
}

inline fun ControllerWrapper.block(
    request: HttpServletRequest,
    response: HttpServletResponse,
    crossinline block: suspend ControllerContext.() -> Unit
) {
    val context = ControllerContext(request, response)
    try {
        runBlocking { context.block() }
        if (!context.isFinished) {
            context.finish()
        }
    } catch (e: ErrorDataTransferObject) {
        // 직접 등록했을 비지니스 로직의 오류만 응답으로 전송. 이외의 오류는 계속 throw.
        context.respondError(e)
    }
}

inline fun <reified R> ControllerWrapper.flux(
    request: HttpServletRequest,
    response: HttpServletResponse,
    block: ControllerContext.() -> Pair<R, HttpStatusCode>
): ResponseEntity<Any> {
    val context = ControllerContext(request, response)
    val result = runCatching { context.block() }
    val exception = result.exceptionOrNull()
    if (exception == null) {
        val (body, status) = result.getOrThrow()
        val serialized = ControllerWrapper.defaultJson.encodeToJsonElement(body)
        return ResponseEntity(serialized, status)
    } else {
        return context.fluxError(exception)
    }
}

fun ControllerContext.fluxError(exception: Throwable): ResponseEntity<Any> {
    if (exception is ErrorDataTransferObject) {
        val body = Response.Error(acceptedAt, exception)
        val status = HttpStatusCode.valueOf(exception.httpStatusCode)
        val serialized = ControllerWrapper.defaultJson.encodeToJsonElement(body)
        return ResponseEntity(serialized, status)
    } else {
        val body = Response.Error(acceptedAt, exception)
        val status = HttpStatus.INTERNAL_SERVER_ERROR
        val serialized = ControllerWrapper.defaultJson.encodeToJsonElement(body)
        return ResponseEntity(serialized, status)
    }
}

inline fun ControllerWrapper.respondEmpty(
    servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse,
    block: () -> Unit
) {
    val acceptedAt = Clock.System.now()
    try {
        block()

        val response = Response.Empty(acceptedAt)
        val encoded = ControllerWrapper.defaultJson.encodeToString(response)
        servletResponse.outputStream.write(encoded.toByteArray())
        servletResponse.contentType = "application/json"
    } catch (e: Throwable) {
        val response = Response.Error(acceptedAt, e)
        val encoded = ControllerWrapper.defaultJson.encodeToString(response)
        servletResponse.status = e.httpStatueCode()
        servletResponse.outputStream.write(encoded.toByteArray())
    }
}

inline fun <reified T : Any> ControllerWrapper.respondObject(
    servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse,
    block: () -> T
) {
    val acceptedAt = Clock.System.now()
    try {
        val response = Response.Object(acceptedAt, block())
        val encoded = ControllerWrapper.defaultJson.encodeToString(response)
        servletResponse.outputStream.write(encoded.toByteArray())
        servletResponse.contentType = "application/json"
    } catch (e: Throwable) {
        val response = Response.Error(acceptedAt, e)
        val encoded = ControllerWrapper.defaultJson.encodeToString(response)
        servletResponse.status = e.httpStatueCode()
        servletResponse.outputStream.write(encoded.toByteArray())
    }
}

inline fun <reified T> ControllerWrapper.respondArray(
    servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse,
    block: () -> List<T>
) {
    val acceptedAt = Clock.System.now()
    try {
        val response = Response.Object(acceptedAt, block())
        val encoded = ControllerWrapper.defaultJson.encodeToString(response)
        servletResponse.outputStream.write(encoded.toByteArray())
        servletResponse.contentType = "application/json"
    } catch (e: Throwable) {
        val response = Response.Error(acceptedAt, e)
        val encoded = ControllerWrapper.defaultJson.encodeToString(response)
        servletResponse.status = e.httpStatueCode()
        servletResponse.outputStream.write(encoded.toByteArray())
    }
}

inline fun ControllerWrapper.respondError(
    servletRequest: HttpServletRequest,
    servletResponse: HttpServletResponse,
    block: () -> Throwable
) {
    val acceptedAt = Clock.System.now()
    val throwable = block()
    val response = Response.Error(acceptedAt, throwable)
    val encoded = ControllerWrapper.defaultJson.encodeToString(response)
    servletResponse.status = throwable.httpStatueCode()
    servletResponse.outputStream.write(encoded.toByteArray())
    servletResponse.contentType = "application/json"
}