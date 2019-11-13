package com.decathlon.consumer

import com.decathlon.Topic
import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.JsonObject
import io.vertx.kafka.client.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

class ConsumerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(ConsumerVerticle::class.java)

    private var config = mutableMapOf<String, String?>()

    override fun start() {
        initConfig(config().getJsonObject("consumer"))
        val eventBus = vertx.eventBus()
        val consumer = KafkaConsumer.create<String, Any>(vertx, config)
            .subscribe(Topic.ITEM_MAINTENANCE.key){
                if (it.succeeded()) {
                    logger.info("subscribe succeed: ${Topic.ITEM_MAINTENANCE.key}")
                } else {
                    logger.error("subscribe failed: ${it.cause()}")
                }
            }
            consumer.handler{ record ->
                logger.info("Processing key=${record.key()},value=${record.value()},partition=${record.partition()},offset=${record.offset()}")
                val deliveryOption = DeliveryOptions().setSendTimeout(300_000L)
                eventBus.request<String>(config().getJsonObject("eventBus").getString("client", "client"), record.value(), deliveryOption){ ar ->
                    if (ar.succeeded() && "OK" == ar.result().body()) {
                        consumer.commit {
                            if (it.succeeded()) {
                                logger.info("committed")
                            } else {
                                logger.error("commit failed: ", it.cause())
                            }
                        }
                    } else {
                        logger.error("consume message failed: ${if(ar.succeeded()) ar.result().body() else ar.cause() }")
                    }
                }

            }

        logger.info("consumer started")
    }

    private fun initConfig(cfg: JsonObject){
        cfg.forEach {
            config[it.key] = it.value?.toString()
        }
    }
}