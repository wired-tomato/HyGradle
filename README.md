# HyGradle
### A Hytale plugin for Gradle

Simple and convenient to use, simple install it and it will:
- download the hytale server downloader
- use the server downloader to download the hytale server (Unsupported on MacOS, turn on `useUserHytaleJar` option)
- setup all necessary gradle configurations and runs

## [Plugin Template](https://github.com/wired-tomato/hytale-plugin-template)

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
    id("net.wiredtomato.hygradle") version "0.2.4"
}

hytale {
    //patchLine = "release"
    //runDirectory = "run"

    //strip hytale jar down to only classes in the com.hytale package and download dependencies seperately
    //stripHytaleJar = true

    //copy auth.enc file to and from Gradle user home
    //useUserHomeAuth = true

    //download Hytale to Gradle user home
    //downloadToUserHome = true

    //amount of time the server jar will persist without being updated
    //serverLifetime = Duration.ofHours(48)

    //serverArgs.addAll("--bind", "127.0.0.1:7878")
    //serverJvmArgs.addAll("-Xmx2G")
    //additionalModPaths.addAll("path/to/local/mods")

    //use local Hytale install instead of fetching via Hytale server downloader
    //required for MacOS
    //useUserHytaleJar = false

    //specify Hytale install dir for "useUserHytaleJar" option
    //defaults to Hytale launcher default
    //hytaleInstallDir = project.file("path/to/local/install")
}
```

**thats it!**

Run the server with `runServer` task or

Invalidate your server jar with the `invalidateHytaleDownload` task

Decompile the Hytale server files with the `genSources` task (Uses Vineflower)

Invalidate auth.enc file with the `invalidateCredentials` task

### This was mostly made for some fun, feel free to report any bugs or issues, or even make feature requests!
