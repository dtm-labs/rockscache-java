plugins {
    kotlin("jvm") version "1.6.10"
}

group = "io.github.dtm-labs"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("redis.clients:jedis:4.2.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("org.slf4j:slf4j-api:1.7.36")

    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:slf4j-log4j12:1.7.36")
}