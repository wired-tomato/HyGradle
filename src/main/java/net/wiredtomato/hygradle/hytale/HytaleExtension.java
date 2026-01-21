package net.wiredtomato.hygradle.hytale;

import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.io.File;
import java.time.Duration;

public abstract class HytaleExtension {
    public final Property<String> patchLine;
    public final Property<File> runDirectory;
    public final Property<Boolean> stripHytaleJar;
    public final Property<Boolean> useUserHomeAuth;
    public final Property<Boolean> downloadToUserHome;
    public final Property<Duration> serverLifetime;
    public final ListProperty<String> serverArgs;
    public final ListProperty<String> serverJvmArgs;
    public final ListProperty<String> additionalModPaths;

    public final Property<File> hytaleInstallDir;
    public final Property<Boolean> useUserHytaleJar;

    @Inject
    public HytaleExtension(Project project) {
        var objects = project.getObjects();
        patchLine = objects.property(String.class);
        runDirectory = objects.property(File.class);
        stripHytaleJar = objects.property(Boolean.class);
        useUserHomeAuth = objects.property(Boolean.class);
        downloadToUserHome = objects.property(Boolean.class);
        serverLifetime = objects.property(Duration.class);
        serverArgs = objects.listProperty(String.class);
        serverJvmArgs = objects.listProperty(String.class);
        additionalModPaths = objects.listProperty(String.class);

        hytaleInstallDir = objects.property(File.class);
        useUserHytaleJar = objects.property(Boolean.class);

        patchLine.set("release");
        runDirectory.set(project.file("run"));
        stripHytaleJar.set(true);
        downloadToUserHome.set(true);
        useUserHomeAuth.set(true);
        serverLifetime.set(Duration.ofHours(48));

        var userHomeDir = System.getProperty("user.home");
        var os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            hytaleInstallDir.set(new File(userHomeDir, "AppData/Roaming/Hytale"));
        } else if (os.contains("mac")) {
            hytaleInstallDir.set(new File(userHomeDir, "Library/Application Support/Hytale"));
        } else {
            var xdgHome = System.getenv("XDG_DATA_HOME");
            if (xdgHome == null || xdgHome.isEmpty()) {
                hytaleInstallDir.set(new File(userHomeDir, ".local/share/Hytale"));
            } else hytaleInstallDir.set(new File(xdgHome, "Hytale"));
        }

        useUserHytaleJar.set(false);

        serverArgs.addAll("--allow-op", "--disable-sentry");
    }
}
