package net.wiredtomato.hygradle.hytale.task;

import net.wiredtomato.hygradle.hytale.HytaleServerDownloader;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;

public abstract class InvalidateHytaleDownloadTask extends DefaultTask {

    @TaskAction
    public void invalidateHytaleDownloadTask() {
        try {
            Files.deleteIfExists(HytaleServerDownloader.getOutputJar());
            Files.deleteIfExists(HytaleServerDownloader.getAssetsZip());
            Files.deleteIfExists(HytaleServerDownloader.getOutputDirectory().resolve(".lastUpdated"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
