plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
}

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("redis.clients:jedis:4.2.3")
    api(project(":rockscache-core"))
    api("org.springframework.boot:spring-boot-starter-data-redis:2.7.0")
}