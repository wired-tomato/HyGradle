package net.wiredtomato.hygradle.hytale;

import net.wiredtomato.hygradle.hytale.task.InvalidateHytaleDownloadTask;
import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class HytaleServerDownloader {
    private static Path outputDirectory;
    private static Path serverDirectory;
    private static Path outputJar;
    private static Path assetsZip;

    public static void register(Project project) {
        var invalidateHytaleDownloadTask = project.getTasks().register("invalidateHytaleDownload", InvalidateHytaleDownloadTask.class);
        invalidateHytaleDownloadTask.get().setGroup("hygradle");
    }

    public static void download(Project project) {
        outputDirectory = project.getLayout().getBuildDirectory().dir("hygradle/download").get().getAsFile().toPath();
        serverDirectory = project.getLayout().getBuildDirectory().file("hygradle/server").get().getAsFile().toPath();

        try {
            Files.createDirectories(outputDirectory);
            Files.createDirectories(serverDirectory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var status = status(project);

        if (status.upToDate) {
            outputJar = status.output;
            assetsZip = assetsZipOutput(status.version, status.patchline);
            return;
        }

        System.out.println("[HyGradle] Downloading Hytale Server...");

        var hytaleExtension = (HytaleExtension) project.getExtensions().getByName("hytale");

        var dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        var timePath = outputDirectory.resolve(".lastUpdated");

        downloadDownloader();
        var os = OperatingSystem.current();


        if (os.isMacOsX()) {
            throw new IllegalStateException("Hytale Downloader does not support Mac OS X");
        }

        var serverZipPath = outputDirectory.resolve("hytale-server.zip");
        var tempServerJarPath = outputDirectory.resolve("HytaleServer-full.jar");

        Path executable = os.isWindows()
                ? outputDirectory.resolve("downloader/hytale-downloader-windows-amd64.exe")
                : outputDirectory.resolve("downloader/hytale-downloader-linux-amd64");

        var patchLine = hytaleExtension.patchLine.get();

        var proc = new ProcessBuilder()
                .directory(outputDirectory.toFile())
                .command("\"" + executable + "\"", "-patchline", patchLine, "-download-path", "\"" + serverZipPath.toAbsolutePath() + "\"", "-skip-update-check")
                .redirectErrorStream(true);

        Process process;
        try {
            process = proc.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream is = process.getInputStream()) {
            int b;
            while((b = is.read()) != -1) {
                System.out.write(b);
                if (b == '\n' || b == '\r') System.out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println();

        String hytaleVersion;

        try (ZipFile serverZip = new ZipFile(serverZipPath.toFile())) {
            var jar = serverZip.getEntry("Server/HytaleServer.jar");

            try (InputStream is = serverZip.getInputStream(jar)) {
                try (OutputStream out = Files.newOutputStream(tempServerJarPath)) {
                    is.transferTo(out);
                }
            }

            hytaleVersion = HytaleVersionExtractor.getHytaleVersion(tempServerJarPath);
            assetsZip = assetsZipOutput(hytaleVersion, patchLine);

            var assets = serverZip.getEntry("Assets.zip");

            try (InputStream is = serverZip.getInputStream(assets)) {
                try (OutputStream out = Files.newOutputStream(assetsZip)) {
                    is.transferTo(out);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (ZipFile tempServerJar = new ZipFile(tempServerJarPath.toFile())) {
            Files.writeString(getServerDir().resolve(".version"), hytaleVersion);

            var strippedOutput = serverJarOutput(hytaleVersion, patchLine, true);
            var unstrippedOutput = serverJarOutput(hytaleVersion, patchLine, false);

            outputJar = hytaleExtension.stripHytaleJar.get() ? strippedOutput : unstrippedOutput;

            Files.createDirectories(outputJar.getParent());

            try (ZipOutputStream serverJar = new ZipOutputStream(new FileOutputStream(strippedOutput.toFile()))) {
                var entries = tempServerJar.entries();

                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();

                    if (shouldCopyEntry(entry)) {
                        serverJar.putNextEntry(new ZipEntry(entry.getName()));
                        try (var is = tempServerJar.getInputStream(entry)) {
                            is.transferTo(serverJar);
                        }
                    }

                }
            }

            try (ZipOutputStream serverJar = new ZipOutputStream(new FileOutputStream(unstrippedOutput.toFile()))) {
                var entries = tempServerJar.entries();

                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();

                    serverJar.putNextEntry(new ZipEntry(entry.getName()));
                    try (var is = tempServerJar.getInputStream(entry)) {
                        is.transferTo(serverJar);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Files.writeString(getServerDir().resolve(".patchline"), patchLine);
            Files.writeString(timePath, dateTimeFormatter.format(LocalDateTime.now()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path serverJarOutput(String version, String patchline, Boolean stripped) {
        version = patchline + "." + version;

        if (stripped) {
            version += "-stripped";
        }

        return getServerDir().resolve("hytale-server-" + version + ".jar");
    }

    private static Path assetsZipOutput(String version, String patchline) {
        version = patchline + "." + version;

        return getServerDir().resolve("assets-" + version + ".zip");
    }

    private static boolean shouldCopyEntry(ZipEntry entry) {
        return entry.getName().contains("com/hypixel") || entry.getName().contains("migration") || entry.getName().contains("META-INF") || entry.getName().equals("manifests.json");
    }

    private static void downloadDownloader() {
        var client = HttpClient.newHttpClient();

        var request = HttpRequest.newBuilder(URI.create("https://downloader.hytale.com/hytale-downloader.zip"))
                .GET()
                .build();

        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        try (var zip = new ZipInputStream(response.body())) {
            Files.createDirectories(outputDirectory.resolve("downloader"));

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || entry.getName().equals("QUICKSTART.md")) {
                    continue;
                }

                try (var output = new FileOutputStream(outputDirectory.resolve("downloader").resolve(entry.getName()).toFile())) {
                    zip.transferTo(output);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        outputDirectory.resolve("downloader/hytale-downloader-linux-amd64").toFile().setExecutable(true);

        client.close();
    }

    public record Status(Boolean upToDate, String version, String patchline, Path output) {}

    public static Status status(Project project) {
        var hytaleExtension = (HytaleExtension) project.getExtensions().getByName("hytale");

        var dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        var timePath = outputDirectory.resolve(".lastUpdated");

        String version;
        String patchline;
        try {
            version = Files.readString(getServerDir().resolve(".version"));
            patchline = Files.readString(getServerDir().resolve(".patchline"));
        } catch (IOException e) {
            version = "";
            patchline = "";
        }

        if (!patchline.equals(hytaleExtension.patchLine.get())) {
            return new Status(false, version, null, null);
        }

        var selected = serverJarOutput(version, patchline, hytaleExtension.stripHytaleJar.get());
        var selectedAssets = assetsZipOutput(version, patchline);

        if (Files.exists(selected) && Files.exists(selectedAssets)) {
            LocalDateTime time;
            try {
                var timeStr = Files.readString(timePath);
                time = LocalDateTime.parse(timeStr, dateTimeFormatter);
            } catch (IOException e) {
                time = LocalDateTime.MIN;
            }

            var now = LocalDateTime.now();

            return new Status(Duration.between(now, time).compareTo(hytaleExtension.serverLifetime.get()) < 0, version, patchline, selected);
        }

        return new Status(false, version, patchline, selected);
    }

    public static Path getOutputDirectory() {
        return outputDirectory;
    }

    public static Path getOutputJar() {
        return outputJar;
    }

    public static Path getOutputForceStrippedJar() {
        return outputJar.getFileName().toString().contains("stripped")
                ? outputJar
                : getServerDir().resolve(outputJar.getFileName().toString().replace(".jar", "-stripped.jar"));
    }

    public static Path getServerDir() {
        return serverDirectory;
    }

    public static Path getAssetsZip() {
        return assetsZip;
    }
}
