@file:Suppress("DuplicatedCode")

package studio.hcmc.spring.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.http.HttpStatus
import studio.hcmc.kotlin.protocol.io.ErrorDataTransferObject
import studio.hcmc.kotlin.protocol.io.Response

interface ControllerWrapper {
    companion object {
        var defaultJson = Json
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
    block: ControllerContext.() -> Unit
) {
    val context = ControllerContext(request, response)
    try {
        context.block()
        if (!context.isFinished) {
            context.finish()
        }
    } catch (e: ErrorDataTransferObject) {
        // 직접 등록했을 비지니스 로직의 오류만 응답으로 전송. 이외의 오류는 계속 throw.
        context.respondError(e)
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
}