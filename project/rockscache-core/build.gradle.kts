plugins {
    kotlin("jvm") version "1.6.10"
    id("maven-publish")
    id("signing")
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
    api(kotlin("reflect"))

    implementation("redis.clients:jedis:4.2.3")
    implementation("io.lettuce:lettuce-core:6.1.8.RELEASE")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    implementation("org.slf4j:slf4j-api:1.7.36")

    testImplementation(kotlin("test"))
    testImplementation("org.slf4j:slf4j-log4j12:1.7.36")
}

java {
    withSourcesJar()
    withJavadocJar()
}

// Publish to maven-----------------------------------------------------
val DTM_USERNAME: String by project
val DTM_PASSWORD: String by project

publishing {
    repositories {
        maven {
            credentials {
                username = DTM_USERNAME
                password = DTM_PASSWORD
            }
            name = "MavenCentral"
            url = if (project.version.toString().endsWith("-SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
            } else {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            artifactId = "rockscache-core"
            from(components["java"])
            pom {
                name.set("rockscache")
                description.set("SQL DSL base on kotlin")
                url.set("https://github.com/dtm-labs/rockscache-java")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/dtm-labs/rockscache-java/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("babyfish-ct")
                        name.set("陈涛")
                        email.set("babyfish.ct@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/dtm-labs/rockscache-java.git")
                    developerConnection.set("scm:git:ssh://github.com/dtm-labs/rockscache-java.git")
                    url.set("https://github.com//dtm-labs/rockscache-java")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}