plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "net.wiredtomato"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {

}

gradlePlugin {
    plugins {
        create("hygradle") {
            id = "net.wiredtomato.hygradle"
            implementationClass = "net.wiredtomato.hygradle.HyGradlePlugin"
        }
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
        maven("https://maven.teamvoided.org/releases") {
            name = "TeamVoided"
            credentials {
                username = System.getenv("TEAMVOIDED_USER")
                password = System.getenv("TEAMVOIDED_PASS")
            }
        }
    }
}
