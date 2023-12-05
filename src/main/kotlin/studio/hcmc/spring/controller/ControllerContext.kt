package studio.hcmc.spring.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import org.springframework.http.HttpStatus
import studio.hcmc.kotlin.protocol.io.ErrorDataTransferObject
import studio.hcmc.kotlin.protocol.io.Response

class ControllerContext(
    val request: HttpServletRequest,
    val response: HttpServletResponse
) {
    val acceptedAt = Clock.System.now()
    var isFinished: Boolean = false

    fun respondEmpty(status: HttpStatus) {
        checkFinished()
        response.status = status.value()
        writeToResponse(Response.Empty(acceptedAt))
    }

    inline fun <reified T> respondObject(status: HttpStatus, result: T) {
        checkFinished()
        response.status = status.value()
        writeToResponse(Response.Object(acceptedAt, result))
    }

    inline fun <reified T> respondArray(status: HttpStatus, result: List<T>) {
        checkFinished()
        response.status = status.value()
        writeToResponse(Response.Array(acceptedAt, result))
    }

    fun respondError(result: Throwable) {
        checkFinished()
        if (result is ErrorDataTransferObject) {
            respondError(HttpStatus.valueOf(result.httpStatusCode), result)
        } else {
            respondError(HttpStatus.INTERNAL_SERVER_ERROR, result)
        }
    }

    fun respondError(status: HttpStatus, result: Throwable) {
        checkFinished()
        response.status = status.value()
        writeToResponse(Response.Error(acceptedAt, result))
    }

    fun finish() {
        respondEmpty(HttpStatus.OK)
    }

    inline fun <reified T> writeToResponse(body: T) {
        response.outputStream.write(encodeToBytes(body))
        response.contentType = "application/json"
        isFinished = true
    }

    inline fun <reified T> encodeToBytes(body: T): ByteArray {
        return ControllerWrapper.defaultJson.encodeToString(body).toByteArray()
    }

    fun checkFinished() {
        if (isFinished) {
            throw IllegalStateException("Already responded.")
        }
    }
}