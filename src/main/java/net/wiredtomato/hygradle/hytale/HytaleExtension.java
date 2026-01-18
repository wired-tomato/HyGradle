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
    public final Property<Duration> serverLifetime;
    public final ListProperty<String> serverArgs;
    public final ListProperty<String> serverJvmArgs;
    public final ListProperty<String> additionalModPaths;

    @Inject
    public HytaleExtension(Project project) {
        var objects = project.getObjects();
        patchLine = objects.property(String.class);
        runDirectory = objects.property(File.class);
        stripHytaleJar = objects.property(Boolean.class);
        serverLifetime = objects.property(Duration.class);
        serverArgs = objects.listProperty(String.class);
        serverJvmArgs = objects.listProperty(String.class);
        additionalModPaths = objects.listProperty(String.class);

        patchLine.set("release");
        runDirectory.set(project.file("run"));
        stripHytaleJar.set(true);
        serverLifetime.set(Duration.ofHours(48));
    }
}
