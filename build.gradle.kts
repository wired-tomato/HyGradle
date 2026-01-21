import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.1"
}

group = "net.wiredtomato"
version = "0.2.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.vineflower:vineflower:1.11.2")
}

gradlePlugin {
    plugins {
        create("hygradle") {
            id = "net.wiredtomato.hygradle"
            implementationClass = "net.wiredtomato.hygradle.HyGradlePlugin"
        }
    }
}

tasks.getByName<ShadowJar>("shadowJar") {
    archiveClassifier.set("")

    relocate("net.fabricmc.fernflower", "net.wiredtomato.hygradle.fabricmc.fernflower")
    relocate("org.jetbrains.java.decompiler", "net.wiredtomato.hygradle.jetbrains.decompiler")
}

tasks.jar {
    enabled = false
}

configurations.runtimeElements {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.shadowJar)
}
configurations.apiElements {
    outgoing.artifacts.clear()
    outgoing.artifact(tasks.shadowJar)
}

components.named<AdhocComponentWithVariants>("java") {
    withVariantsFromConfiguration(configurations.runtimeElements.get()) {
        skip()
    }
}

var props = mapOf(
    "version" to version,
)

tasks.processResources {
    inputs.properties(props)

    filesMatching(listOf("net/wiredtomato/hygradle/version/version")) {
        expand(props)
    }
}

publishing {
    repositories {
        mavenLocal()
        maven("https://maven.teamvoided.org/releases") {
            name = "TeamVoided"
            credentials {
                username = System.getenv("TEAMVOIDED_USER")
                password = System.getenv("TEAMVOIDED_PASS")
            }
        }
    }
}
