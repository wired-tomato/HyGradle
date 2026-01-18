package net.wiredtomato.hygradle;

import net.wiredtomato.hygradle.hytale.HytaleExtension;
import net.wiredtomato.hygradle.hytale.HytaleServerDownloader;
import net.wiredtomato.hygradle.hytale.HytaleVersionExtractor;
import net.wiredtomato.hygradle.hytale.task.RunServerTask;
import net.wiredtomato.hygradle.version.Version;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

public class HyGradlePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("[HyGradle] Running HyGradle version " + Version.VERSION);

        HytaleExtension hytaleExtension = project.getExtensions().create("hytale", HytaleExtension.class);
        HytaleServerDownloader.register(project);

        project.afterEvaluate(proj -> {
            HytaleServerDownloader.download(proj);

            var hytaleVersion = HytaleVersionExtractor.getHytaleVersion(HytaleServerDownloader.getOutputJar());
            System.out.println("[HyGradle] Hytale version " + hytaleVersion);
            if (hytaleExtension.stripHytaleJar.get()) System.out.println("[HyGradle] The Hytale jar has been stripped to only contain com.hypixel classes");

            applyDependencies(proj, hytaleExtension, hytaleVersion);

            var runServerTask = project.getTasks().register("runServer", RunServerTask.class);
            runServerTask.get().setGroup("hygradle");
        });
    }

    private void applyDependencies(Project project, HytaleExtension hytaleExtension, String hytaleVersion) {
        var dependencies = project.getDependencies();

        project.getRepositories().flatDir((d) -> d.dirs(HytaleServerDownloader.getServerDir()));

        String requestedVersion = hytaleExtension.patchLine.get() + "." + (hytaleExtension.stripHytaleJar.get() ? hytaleVersion + "-stripped" : hytaleVersion);

        project.getConfigurations().getByName("compileOnly").resolutionStrategy(resolutionStrategy -> resolutionStrategy.dependencySubstitution(dep -> dep.substitute(dep.module("com.hypixel:hytale-server"))
                .using(dep.module("local:hytale-server:" + requestedVersion))));

        dependencies.add("compileOnly", "com.hypixel:hytale-server:" + requestedVersion);

        if (!hytaleExtension.stripHytaleJar.get()) return;

        impl(dependencies, "ch.randelshofer:fastdoubleparser:2.0.1");
        impl(dependencies, "com.github.luben:zstd-jni:1.5.7-6");
        impl(dependencies, "com.google.flogger:flogger:0.9");
        impl(dependencies, "com.google.flogger:flogger-system-backend:0.9");
        impl(dependencies, "com.google.crypto.tink:tink:1.7.0");
        impl(dependencies, "com.google.errorprone:error_prone_core:2.46.0");
        impl(dependencies, "com.google.code.gson:gson:2.13.2");
        impl(dependencies, "com.google.protobuf:protobuf-java:4.33.4");
        impl(dependencies, "com.nimbusds:nimbus-jose-jwt:10.7");
        impl(dependencies, "io.netty:netty-all:4.2.9.Final");
        impl(dependencies, "io.sentry:sentry:8.30.0");
        impl(dependencies, "it.unimi.dsi:fastutil:8.5.18");
        impl(dependencies, "javax.annotation:javax.annotation-api:1.3.2");
        impl(dependencies, "net.sf.jopt-simple:jopt-simple:5.0.4");
        impl(dependencies, "net.sf.jopt-simple:jopt-simple:5.0.4");
        impl(dependencies, "org.bouncycastle:bcprov-jdk18on:1.83");
        impl(dependencies, "org.bouncycastle:bcpkix-jdk18on:1.83");
        impl(dependencies, "org.mongodb:bson:5.6.2");
        impl(dependencies, "org.checkerframework:checker-compat-qual:2.5.6");
        impl(dependencies, "org.fusesource.jansi:jansi:2.4.2");
        impl(dependencies, "org.jline:jline:3.30.6");
    }

    private void impl(DependencyHandler dependencies, Object notation) {
        dependencies.add("implementation", notation);
    }
}
