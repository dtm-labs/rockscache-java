plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.springframework.boot:spring-boot-starter-data-redis:2.7.0")
    implementation("redis.clients:jedis:4.2.3")
    api(project(":rockscache-core"))
}