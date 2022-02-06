package com.levels.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import java.util.*

class Producer {
    fun <K, V> createProducer(props: Properties): KafkaProducer<K, V> {
        return KafkaProducer(props)
    }
}
