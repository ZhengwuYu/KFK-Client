package com.decathlon.producer

import com.decathlon.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import io.vertx.kafka.client.producer.KafkaProducer
import io.vertx.kafka.client.producer.KafkaProducerRecord
import org.slf4j.LoggerFactory

/**
 * Producer class to handle the request to generate the message and then send to Kafka
 */
class ProducerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(ProducerVerticle::class.java)

    // configuration for Kafka producer
    private var config = mutableMapOf<String, String?>()

    override fun start() {
        initConfig(config().getJsonObject("producer"))
        val eventBus = vertx.eventBus()
        // event bus to consume the message from web server(flow request)
        eventBus.consumer<JsonObject>("service"){ msgObj ->
            val message = msgObj.body()
            // create the Kafka record
            val record = KafkaProducerRecord.create<String, Any>(Topic.ITEM_MAINTENANCE.key, message)

            // use producer for interacting with Apache Kafka
            val producer = KafkaProducer.create<String, Any>(vertx, config)

            // log exception
            producer.exceptionHandler {
                logger.error("error occurs", it)
                msgObj.reply("sending Kafka failed by: ${it.cause}")
            }

            // send record to Kafka
            producer.send(record){
                if (it.succeeded()) {
                    val recordMetadata = it.result()
                    logger.info("Message ${record.value()} written on topic=${recordMetadata.topic}, partition=${recordMetadata.partition}, offset=${recordMetadata.offset}")
                    // close the producer to release resource
                    msgObj.reply("OK")
                    closeProducer(producer)
                } else {
                    msgObj.reply(it.cause().message)
                    logger.error("message sent failed: ${it.cause()}")
                }
            }
        }
        logger.info("producer started")
    }

    /**
     * close the producer
     * @param producer target need to be closed
     */
    private fun closeProducer(producer: KafkaProducer<String, Any>){
        producer.close{
            if (it.succeeded()) {
                logger.info("producer closed.")
            }else{
                logger.error("producer close failed: ${it.cause()}")
            }
        }
    }

    private fun initConfig(cfg: JsonObject){
        cfg.forEach {
            config[it.key] = it.value?.toString()
        }
    }

}
