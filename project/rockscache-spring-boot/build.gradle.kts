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
    api(project(":rockscache-core"))
    api("org.springframework.boot:spring-boot-starter-data-redis:2.7.0")
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
            artifactId = "rockscache-spring-boot"
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