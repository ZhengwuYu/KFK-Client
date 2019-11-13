package com.decathlon

import com.decathlon.client.WebClientVerticle
import com.decathlon.consumer.ConsumerVerticle
import com.decathlon.server.ServerVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import org.slf4j.LoggerFactory

class MainConsumerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(MainConsumerVerticle::class.java)
    override fun start() {
        vertx.deployVerticle(WebClientVerticle(), DeploymentOptions().setConfig(config())){ webClientAr ->
            if (webClientAr.succeeded()) {
                val webClientId = webClientAr.result()
                logger.info("web client deployed: $webClientId")
                vertx.deployVerticle(ConsumerVerticle(), DeploymentOptions().setConfig(config())) { consumerAr ->
                    if (consumerAr.succeeded()) {
                        logger.info("consumer deployed: ${consumerAr.result()}")
                    } else {
                        logger.error("deploy consumer failed", consumerAr.cause())
                        vertx.undeploy(webClientId) {
                            logger.info("un-deploy web client since the error")
                        }
                    }
                }
            } else {
                logger.error("deploy web client failed", webClientAr.cause())
            }
        }
    }
}