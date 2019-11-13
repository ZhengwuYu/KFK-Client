package com.decathlon.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class ServerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(ServerVerticle::class.java)
    override fun start() {
        val address = config().getJsonObject("eventBus").getString("service", "service")
        val eventBus = vertx.eventBus()
        // create a http server to handle the
        vertx.createHttpServer()
            .requestHandler { request ->
                val message = getMessage(request)
                request.bodyHandler {buffer ->
                    message.put("body", buffer.toString())
                    eventBus.request<String>(address, message){ ar ->
                        if (ar.succeeded() && "OK" == ar.result().body()) {
                            request.response().setStatusCode(200).end()
                        } else {
                            request.response().setStatusCode(500).end(if (ar.succeeded()) ar.result().body() else ar.cause().message)
                        }
                    }
                }
            }
            .listen(config().getJsonObject("webServer").getInteger("port", 18080))

    }

    private fun getMessage(request: HttpServerRequest): JsonObject {
        val message = JsonObject()
        message.put("uriAbs", request.absoluteURI())
        val headers = JsonObject()
        request.headers().forEach { headers.put(it.key, it.value) }
        val params = JsonObject()
        request.params().forEach { params.put(it.key, it.value) }
        message.put("headers", headers)
        message.put("params", params)
        message.put("uri", request.uri())
        message.put("method", request.method().name)
        return message
    }
}