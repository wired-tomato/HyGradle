package net.wiredtomato.hygradle.hytale.task;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import net.wiredtomato.hygradle.HyGradlePlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FetchServerCredentials extends DefaultTask {
    @TaskAction
    @SuppressWarnings("unchecked")
    public void fetchServerCredentials() {
        var serverConfigFile = serverConfigFile();
        var globalCredentialsFile = globalCredentialsFile();
        var serverCredentialsFile = serverCredentialsFile();

        if (!Files.exists(globalCredentialsFile)) {
            System.out.println("[HyGradle] Failed to fetch server credentials.");
            return;
        }

        Map<String, Object> serverConfig;
        try {
            serverConfig = (Map<String, Object>) new JsonSlurper().parse(serverConfigFile);
        } catch (IOException e) {
            serverConfig = new HashMap<>();
        }

        Map<String, Object> authConfig = (Map<String, Object>) serverConfig.computeIfAbsent("AuthCredentialStore", (it) -> new HashMap<>());

        authConfig.put("Type", "Encrypted");
        authConfig.put("Path", "auth.enc");

        try {
            Files.writeString(serverConfigFile, JsonOutput.prettyPrint(JsonOutput.toJson(serverConfig)));

            Files.deleteIfExists(serverCredentialsFile);
            Files.copy(globalCredentialsFile, serverCredentialsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path globalCredentialsFile() {
        return getProject().getGradle().getGradleUserHomeDir().toPath().resolve("hygradle/server/auth/auth.enc");
    }

    public Path serverCredentialsFile() {
        return HyGradlePlugin.getHytaleExtension(getProject()).runDirectory.get().toPath().resolve("auth.enc");
    }

    public Path serverConfigFile() {
        return HyGradlePlugin.getHytaleExtension(getProject()).runDirectory.get().toPath().resolve("config.json");
    }
}
