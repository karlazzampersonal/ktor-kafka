package com.levels.kafka

import kotlinx.coroutines.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.Thread.currentThread
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext

fun <K, V> buildConsumer(bootstrapServers: String, config: ConsumerConfiguration): KafkaConsumer<K, V> {
    val props = Properties()
    props.setProperty("bootstrap.servers", bootstrapServers)
    props.setProperty("allow.auto.create.topics", config.autoCreateTopics.toString())
    props.setProperty("group.id", config.groupId)
    props.setProperty("auto.offset.reset", config.autoOffsetReset)
    props.setProperty("enable.auto.commit", config.enableAutoCommit.toString())
    props.setProperty("auto.commit.interval.ms", config.autoCommitInterval.toString())
    props.setProperty("key.deserializer", config.keyDeserializer)
    props.setProperty("value.deserializer", config.valueDeserializer)
    props.setProperty("max.poll.records", config.maxPoll.toString())
    props.setProperty("security.protocol", config.securityProtocol)
    config.truststoreLocation?.let {  props.setProperty(" ssl.truststore.location", config.truststoreLocation) }
    config.truststorePassword?.let {  props.setProperty(" ssl.truststore.password", config.truststorePassword) }
    config.keystoreLocation?.let {  props.setProperty(" ssl.keystore.location", config.keystoreLocation) }
    config.keystorePassword?.let {  props.setProperty(" ssl.keystore.password", config.keystorePassword) }
    config.keyPassword?.let {  props.setProperty(" ssl.key.password", config.keyPassword) }

    if (config.avroEnabled) {
        props.setProperty("specific.avro.reader", "true")
        props.setProperty("schema.registry.url", config.schemaRegistryUrl)
        props.setProperty("key.subject.name.strategy", config.valueSubjectNamingStrategy)
        props.setProperty("value.subject.name.strategy", config.valueSubjectNamingStrategy)
        if(config.basicAuthCredentialsSource.isNotEmpty()) {
            props.setProperty("basic.auth.credentials.source", config.basicAuthCredentialsSource)
            props.setProperty("basic.auth.user.info", config.basicAuthCredentials)
        }
    }

    return KafkaConsumer(props)
}

fun <K, V> createKafkaConsumer(bootstrapServers: String, consumerConfiguration: ConsumerConfiguration): KafkaConsumer<K, V> {
    val consumer = buildConsumer<K, V>(bootstrapServers, consumerConfiguration)
    consumer.subscribe(consumerConfiguration.topics)
    return consumer
}

lateinit var log: Logger

class ConsumerJob(kafkaConfiguration: KafkaConfiguration, consumerConfiguration: ConsumerConfiguration) : CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val job = Job()
    public val consumer: KafkaConsumer<Any, Any>
    public val groupId: String

    init {
        groupId = consumerConfiguration.groupId
        log = LoggerFactory.getLogger("kafka-consumer-logger-$groupId")
        log.info("Starting kafka consumer job.")
        consumer = createKafkaConsumer(kafkaConfiguration.bootstrapServer, consumerConfiguration)
        log.info("Kafka consumer job started.")
    }

    override val coroutineContext: CoroutineContext get() = job

    suspend fun start(block: suspend () -> Unit) {
        log.info("Starting consumer group $groupId ....")
        while (isActive) {
            try {
                block()
                yield()
            } catch (ex: CancellationException) {
                log.warn("coroutine on ${currentThread().name} cancelled")
            } catch (ex: Exception) {
                log.error("${currentThread().name} failed with {$ex}. Retrying...")
            }
        }
    }

    fun shutdown() {
        log.info("Shutting down consumer group $groupId")
        job.complete()
        consumer.unsubscribe()
        consumer.close(Duration.ofMillis(5000L))
        log.info("Kafka consumer closed for consumer group $groupId.")
    }
}
