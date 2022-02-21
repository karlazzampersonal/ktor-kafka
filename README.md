# Ktor kafka plugin
![Publish](https://github.com/karlazzampersonal/ktor-kafka/actions/workflows/deploy.yml/badge.svg?branch=main)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-yellow.svg)](https://opensource.org/licenses/Apache-2.0)

A Ktor installable plugin for kafka that allows you to start up consumers in coroutine jobs

## Example repos
- TBD

A light repo with multiple producers and consuming messages for both

## Usage
<details><summary>Set up in Kotlin Gradle:</summary>

```kotlin
repositories {
    mavenCentral()
    // Need a GH access token with read package scope
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/karlazzampersonal/ktor-kafka")
        credentials {
            username = props.getProperty("USERNAME")
            password = props.getProperty("TOKEN")
        }
    }
}

dependencies {
    implementation("com.levels:ktor-kafka:$ktor_kafka_version")
}
```
</details>

You can choose to create 0 to N consumer groups, they each run as separate coroutine jobs.

First, Add the feature to your Application module

```kotlin
 
install(Kafka) {
    kafka { bootstrapServer = "localhost:9092" }
    consumer {
        groupId = "a-group"
        topics = listOf("topica")
        keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer"
        valueDeserializer = "org.apache.kafka.common.serialization.StringDeserializer"
    } 
    consumer {
        groupId = "b-group"
        topics = listOf("topicb")
        keyDeserializer = "org.apache.kafka.common.serialization.StringDeserializer"
        valueDeserializer = "org.apache.kafka.common.serialization.IntegerDeserializer"
    }
    //... Add more consumers if needed
}
```
Create a producer:
```kotlin
// First create the producer props
val props = Properties()
props.setProperty("bootstrap.servers", kafkaConfig.bootstrapServer)
props.setProperty("acks", "all")
props.setProperty("retries", "0")
props.setProperty("linger.ms", "1")
props.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
props.setProperty("value.serializer","org.apache.kafka.common.serialization.StringSerializer")

//Then init a new producer with those props
val producer : KafkaProducer<String, String> = Producer().createProducer(props)
```
After installing the feature, start up a consumer in a coroutine job at application startup:
```kotlin
environment.monitor.subscribe(ApplicationStarted) {
    // Fetch consumer jobs by group id
    val a = ConsumerA(getJob(groupId = "a-group"))
    
    // Launch consumer job in a coroutine scope
    launch {
        a.start()
    }
}
```

Here's what ConsumerA looks like:
```kotlin
class ConsumerA(private val job: ConsumerJob) {
    
    suspend fun start() = job.start {
        val records = job.consumer.poll(Duration.ofMillis(1000L))

        for (record in records) {
            //Make sure to cast the kafka record value to the correct type
            val value = record.value() as String
            log.info("Consumer A reading message: $value")
        }
    }
}
```


## License
This project is licensed under the Apache 2.0 license

## Contributing
We welcome any contributions, please submit an issue or PR.

We are still missing:
- Kafka streams/topology
- Tests (yikes)
- Example with avro de/serialization
- Support for protobuf
- Example with protobuf de/serialization