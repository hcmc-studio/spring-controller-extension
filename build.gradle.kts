plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.spring") version "1.9.0"
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.2"
    id("maven-publish")
}

group = "studio.hcmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "studio.hcmc"
            artifactId = "spring-controller-extension"
            version = "0.0.4"
            from(components["java"])
        }
    }
}

dependencies {
    implementation("com.github.hcmc-studio:kotlin-protocol-extension:0.0.4-build-02")

    implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.4.0")
}