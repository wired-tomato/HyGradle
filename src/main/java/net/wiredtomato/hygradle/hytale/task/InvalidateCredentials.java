package net.wiredtomato.hygradle.hytale.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InvalidateCredentials extends DefaultTask {
    @TaskAction
    public void invalidateCredentials() {
        try {
            Files.deleteIfExists(globalCredentialsFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Path globalCredentialsFile() {
        return getProject().getGradle().getGradleUserHomeDir().toPath().resolve("hygradle/server/auth/auth.enc");
    }
}
