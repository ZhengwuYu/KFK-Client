package com.decathlon

import com.decathlon.producer.ProducerVerticle
import com.decathlon.server.ServerVerticle
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import org.slf4j.LoggerFactory

class MainProducerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(MainProducerVerticle::class.java)
    override fun start() {
        vertx.deployVerticle(ProducerVerticle(), DeploymentOptions().setConfig(config())){ producerAr ->
            if (producerAr.succeeded()) {
                val producerId = producerAr.result()
                logger.info("producer deployed: $producerId")
                vertx.deployVerticle(ServerVerticle(), DeploymentOptions().setConfig(config())) { serverAr ->
                    if (serverAr.succeeded()) {
                        logger.info("web server deployed: ${serverAr.result()}")
                    } else {
                        logger.error("deploy web server failed", serverAr.cause())
                        vertx.undeploy(producerId) {
                            logger.info("un-deploy producer since the error")
                        }
                    }
                }
            } else {
                logger.error("deploy producer failed", producerAr.cause())
            }
        }
    }
}