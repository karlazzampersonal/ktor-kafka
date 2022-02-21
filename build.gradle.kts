import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val kafka_version: String by project

plugins {
    kotlin("jvm") version "1.6.10"
    id("java")
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    id("maven-publish")
}

group = "com.levels"
version = "0.0.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    api("org.apache.kafka:kafka-clients:$kafka_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

tasks.test {
    useJUnitPlatform()
    systemProperty("gradle.build.dir", project.buildDir)
}

ktlint {
    disabledRules.set(mutableListOf("no-wildcard-imports"))
    filter {
        exclude("**/generated/**")
        exclude { element -> element.file.path.contains("generated/") }
        include("**/kotlin/**")
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/karlazzampersonal/ktor-kafka")
            credentials {
                username = System.getenv("GH_USER")
                password = System.getenv("GH_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}
