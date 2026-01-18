# HyGradle
### A Hytale plugin for Gradle

Simple and convenient to use, simple install it and it will:
- download the hytale server downloader
- use the server downloader to download the hytale server (side effect: Mac OS is not supported at the moment as the hytale server downloader does not bundle a mac os executable)
- setup all necessary gradle configurations and runs

## Installation
settings.gradle.kts
```kotlin
pluginManagement {
    repositories {
        maven("https://maven.teamvoided.org/releases") {
            name = "TeamVoided"
        }
    }
}
```

build.gradle.kts
```kotlin
plugins {
    id("net.wiredtomato.hygradle") version "0.2.1"
}

hytale {
    //patchLine = "release"
    //runDirectory = "run"
    
    //strip hytale jar down to only classes in the com.hytale package and download dependencies seperately
    //stripHytaleJar = true
    
    //amount of time the server jar will persist without being updated
    //serverLifetime = Duration.ofHours(48)
    
    //serverArgs.addAll("--bind", "127.0.0.1:7878")
    //serverJvmArgs.addAll("-Xmx2G")
    //additionalModPaths.addAll("path/to/local/mods")
}
```

**thats it!**

Run the server with `runServer` task or

Invalidate your server jar with the `invalidateHytaleDownload` task

Decompile the Hytale server files with the `genSources` task (Uses Vineflower)

### This was mostly made for some fun, feel free to report any bugs or issues, or even make feature requests!
