package net.wiredtomato.hygradle.hytale.task;

import net.wiredtomato.hygradle.hytale.HytaleExtension;
import net.wiredtomato.hygradle.hytale.HytaleServerDownloader;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.JavaExec;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

public abstract class RunServerTask extends JavaExec {
    @Inject
    public RunServerTask() {
        var hytaleExtension = (HytaleExtension) getProject().getExtensions().getByName("hytale");
        var javaExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        var compileJava = getProject().getTasks().getByName("compileJava");

        if (hytaleExtension.useUserHomeAuth.get()) {
            var fetchServerCredentialsTask = (FetchServerCredentials) getProject().getTasks().getByName("fetchServerCredentials");
            dependsOn(fetchServerCredentialsTask);

            var globalCreds = fetchServerCredentialsTask.globalCredentialsFile();

            if (!Files.exists(globalCreds)) {
                //Wait for server login then copy to Gradle user home
                new Thread(() -> {
                    var serverCreds = fetchServerCredentialsTask.serverCredentialsFile();

                    try {
                        if (Files.exists(serverCreds)) {
                            Files.copy(serverCreds, globalCreds);
                            System.out.println("[HyGradle] Successfully uploaded server credentials.");
                            return;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
                        serverCreds.getParent().register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

                        boolean found = false;
                        while (!found) {
                            WatchKey key = watcher.take();

                            for (WatchEvent<?> event : key.pollEvents()) {
                                if (event.context().equals(serverCreds.getFileName())) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!key.reset()) {
                                break;
                            }
                        }

                        Files.copy(serverCreds, globalCreds);
                        System.out.println("[HyGradle] Successfully uploaded server credentials.");
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        }

        dependsOn(compileJava);

        hytaleExtension.runDirectory.get().mkdirs();

        workingDir(hytaleExtension.runDirectory.get());

        var mainSources = javaExtension.getSourceSets().getByName("main");

        classpath(mainSources.getRuntimeClasspath());
        args("--assets", "\"" + HytaleServerDownloader.getAssetsZip().toAbsolutePath() + "\"");

        getMainClass().set("com.hypixel.hytale.Main");

        var modPaths = new ArrayList<>(hytaleExtension.additionalModPaths.get());

        modPaths.add(mainSources.getOutput().getResourcesDir().getParentFile().getAbsolutePath());
        args("--mods=\"" + String.join(",", modPaths) + "\"");

        args(hytaleExtension.serverArgs.get());
        jvmArgs(hytaleExtension.serverJvmArgs.get());

        setStandardInput(System.in);
    }
}
