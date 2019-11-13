package com.decathlon

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

class MainVerticle: AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(MainVerticle::class.java)
    override fun start() {
        logger.info("config: ${config()}")
        config().getJsonArray("deploy").forEach{
            if (it is JsonObject) {
                val className = it.getString("class")
                vertx.deployVerticle(className, DeploymentOptions().setConfig(config())){
                    if (it.succeeded()) {
                        logger.info("$className deployed successfully")
                    } else {
                        logger.error("deployment failed for $className", it.cause())
                    }
                }
            }
        }
    }
}