package net.wiredtomato.hygradle.hytale.task;

import net.wiredtomato.hygradle.hytale.HytaleExtension;
import net.wiredtomato.hygradle.hytale.HytaleServerDownloader;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.JavaExec;

import javax.inject.Inject;
import java.util.ArrayList;

public abstract class RunServerTask extends JavaExec {
    @Inject
    public RunServerTask() {
        var hytaleExtension = (HytaleExtension) getProject().getExtensions().getByName("hytale");
        var javaExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
        var compileJava = getProject().getTasks().getByName("compileJava");

        dependsOn(compileJava);

        hytaleExtension.runDirectory.get().mkdirs();

        workingDir(hytaleExtension.runDirectory.get());

        var mainSources = javaExtension.getSourceSets().getByName("main");

        classpath(HytaleServerDownloader.getOutputJar(), mainSources.getRuntimeClasspath());

        getMainClass().set("com.hypixel.hytale.Main");
        args("--allow-op", "--disable-sentry", "--assets", "\"" + HytaleServerDownloader.getAssetsZip().toAbsolutePath() + "\"");

        var modPaths = new ArrayList<>(hytaleExtension.additionalModPaths.get());

        modPaths.add(mainSources.getOutput().getResourcesDir().getParentFile().getAbsolutePath());

        args("--mods=\"" + String.join(",", modPaths) + "\"");

        args(hytaleExtension.serverArgs.get());
        jvmArgs(hytaleExtension.serverJvmArgs.get());

        setStandardInput(System.in);
    }
}
