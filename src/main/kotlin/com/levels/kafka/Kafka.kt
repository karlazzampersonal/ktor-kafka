package com.levels.kafka

import io.ktor.application.*
import io.ktor.util.*

data class KafkaConfiguration(
    var bootstrapServer: String = "localhost:9092"
)

data class ConsumerConfiguration(
    var groupId: String = "",
    var enableAutoCommit: Boolean = true,
    var autoCommitInterval: Int = 1000,
    var keyDeserializer: String = "org.apache.kafka.common.serialization.StringDeserializer",
    var valueDeserializer: String = "org.apache.kafka.common.serialization.StringDeserializer",
    var topics: List<String> = listOf(),
)

class Kafka(configuration: Configuration) {
    internal val kafkaConfiguration = configuration.kafkaConfiguration
    internal val consumerConfigurations = configuration.consumerConfigurations

    class Configuration {
        internal var kafkaConfiguration: KafkaConfiguration = KafkaConfiguration()
        internal var consumerConfigurations = mutableListOf<ConsumerConfiguration>()

        fun addKafkaConfig(configure: KafkaConfiguration) {
            kafkaConfiguration = configure
        }

        fun addConsumer(configuration: ConsumerConfiguration) {
            consumerConfigurations.add(configuration)
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, Kafka> {
        override val key = AttributeKey<Kafka>("Kafka")
        private val consumerJobMap = mutableMapOf<String, ConsumerJob>()
        public val consumerList = mutableListOf<ConsumerJob>()
        public lateinit var kafkaConfig: KafkaConfiguration

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): Kafka {
            val configuration = Configuration().apply(configure)
            val kafka = Kafka(configuration)
            kafkaConfig = kafka.kafkaConfiguration

            for (consumerConfig in kafka.consumerConfigurations) {
                val job = ConsumerJob(kafkaConfig, consumerConfig)
                consumerJobMap[job.groupId] = job
                consumerList.add(job)
            }

            return kafka
        }

        fun getJob(groupId: String): ConsumerJob {
            return consumerJobMap[groupId] ?: throw IllegalArgumentException("Consumer job with $groupId does not exist!")
        }
    }
}

public fun Kafka.Configuration.kafka(configure: KafkaConfiguration.() -> Unit) {
    addKafkaConfig(KafkaConfiguration().apply(configure))
}

public fun Kafka.Configuration.consumer(configure: ConsumerConfiguration.() -> Unit) {
    addConsumer(ConsumerConfiguration().apply(configure))
}
