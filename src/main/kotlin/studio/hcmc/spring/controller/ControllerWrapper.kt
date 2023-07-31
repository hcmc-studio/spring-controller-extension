package studio.hcmc.spring.controller

import kotlinx.datetime.Clock
import studio.hcmc.kotlin.protocol.io.Response

interface ControllerWrapper {
    companion object
}

inline fun ControllerWrapper.respondEmpty(block: () -> Unit): Response.Empty {
    val acceptedAt = Clock.System.now()
    block()

    return Response.Empty(acceptedAt)
}

inline fun <reified T> ControllerWrapper.respondObject(block: () -> T): Response.Object<T> {
    val acceptedAt = Clock.System.now()
    val result = block()

    return Response.Object(acceptedAt, result)
}

inline fun <reified T> ControllerWrapper.respondArray(block: () -> List<T>): Response.Array<T> {
    val acceptedAt = Clock.System.now()
    val result = block()

    return Response.Array(acceptedAt, result)
}

inline fun ControllerWrapper.respondError(block: () -> Throwable): Response.Error {
    val acceptedAt = Clock.System.now()
    val result = block()

    return Response.Error(acceptedAt, result)
}