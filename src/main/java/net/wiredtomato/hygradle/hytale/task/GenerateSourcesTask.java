package net.wiredtomato.hygradle.hytale.task;

import net.wiredtomato.hygradle.hytale.HytaleServerDownloader;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.java.decompiler.api.Decompiler;
import org.jetbrains.java.decompiler.main.decompiler.SingleFileSaver;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

import java.nio.file.Path;

public class GenerateSourcesTask extends DefaultTask {
    private final Path serverSourceJar;
    private final Path serverJar;

    public GenerateSourcesTask() {
        serverSourceJar = HytaleServerDownloader.getServerDir().resolve(HytaleServerDownloader.getOutputJar().getFileName().toString().replace(".jar", "-sources.jar"));
        serverJar = HytaleServerDownloader.getOutputJar();
    }

    @TaskAction
    public void decompileHytale() {
        Decompiler.Builder decompiler = new Decompiler.Builder()
                .inputs(getServerJar().toFile())
                .output(new SingleFileSaver(getServerSourceJar().toFile()))
                .logger(new IFernflowerLogger() {
                    @Override
                    public void writeMessage(String message, Severity severity) {
                        if (severity != Severity.INFO && severity != Severity.TRACE)
                            System.out.println("[" + severity + "] " + message);
                    }

                    @Override
                    public void writeMessage(String message, Severity severity, Throwable t) {
                        System.out.println("[" + severity + "] " + message + ": " + t);
                    }
                });

        IFernflowerPreferences.getDefaults().forEach(decompiler::option);

        decompiler.build().decompile();
    }

    @OutputFile
    public Path getServerSourceJar() {
        return serverSourceJar;
    }

    @InputFile
    public Path getServerJar() {
        return serverJar;
    }
}
