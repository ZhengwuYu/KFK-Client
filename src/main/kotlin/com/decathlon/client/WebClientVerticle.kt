package com.decathlon.client

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.client.WebClientOptions
import org.slf4j.LoggerFactory
import kotlin.math.log

class WebClientVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(WebClientVerticle::class.java)

    override fun start() {
        val cfg = config().getJsonObject("webClient")
        val eventBus = vertx.eventBus()
        eventBus.consumer<JsonObject>(config().getJsonObject("eventBus").getString("client", "client")) { msgObj ->
            val message = msgObj.body()

            val webClientOptions = WebClientOptions()
                .setSsl(cfg.getBoolean("ssl", false))
                .setTrustAll(true)
                .setVerifyHost(false)
//                .setKeepAlive(true)

            val webClient = WebClient.create(vertx, webClientOptions)
            val method = message.getString("method")
            val request = webClient.raw(
                method ?: "GET",
                cfg.getInteger("port", 443),
                cfg.getString("host"),
                message.getString("uri")
            )
                .apply {
                    message.getJsonObject("headers")?.forEach {
                        putHeader(it.key, it.value as String)
                    }

                    message.getJsonObject("params")?.forEach {
                        setQueryParam(it.key, it.value as String)
                    }
                }
            logger.info(request.toString())
            if (message.containsKey("body")) {
                request.sendBuffer(Buffer.buffer(message.getString("body"))) {
                    handler(it, msgObj)
                }
            } else {
                request.send { handler(it, msgObj) }
            }

        }
    }

    private fun handler(it: AsyncResult<HttpResponse<Buffer>>, msgObj: Message<JsonObject>) {
        if (it.succeeded() && it.result().statusCode() == 200) {
            msgObj.reply("OK")
            logger.info("request send successfully")
        } else {
            msgObj.reply(if (it.succeeded()) it.result().statusMessage() else it.cause().message)
            logger.error("request failed:", it.cause())
        }
    }
}