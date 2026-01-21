package net.wiredtomato.hygradle.hytale;

import net.wiredtomato.hygradle.hytale.task.InvalidateHytaleDownloadTask;
import org.gradle.api.Project;
import org.gradle.internal.os.OperatingSystem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private static String hytaleVersion;

    public static void register(Project project) {
        var invalidateHytaleDownloadTask = project.getTasks().register("invalidateHytaleDownload", InvalidateHytaleDownloadTask.class);
        invalidateHytaleDownloadTask.get().setGroup("hygradle");
    }

    public static void download(Project project) {
        var hytaleExtension = (HytaleExtension) project.getExtensions().getByName("hytale");

        if (hytaleExtension.downloadToUserHome.get()) {
            outputDirectory = project.getGradle().getGradleUserHomeDir().toPath().resolve("hygradle/download");
            serverDirectory = project.getGradle().getGradleUserHomeDir().toPath().resolve("hygradle/server");
        } else {
            outputDirectory = project.getLayout().getBuildDirectory().dir("hygradle/download").get().getAsFile().toPath();
            serverDirectory = project.getLayout().getBuildDirectory().file("hygradle/server").get().getAsFile().toPath();
        }

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


        var dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        var timePath = outputDirectory.resolve(".lastUpdated");

        if (hytaleExtension.useUserHytaleJar.get()) {
            var installDir = hytaleExtension.hytaleInstallDir.get().toPath();
            var patchline = hytaleExtension.patchLine.get();
            var userHytaleJar = installDir.resolve("install/" + patchline + "/package/game/latest/Server/HytaleServer.jar");
            var userAssetsZip = userHytaleJar.getParent().getParent().resolve("Assets.zip");


            System.out.println("[HyGradle] Using user Hytale jar, Hytale install directory: " + installDir);

            hytaleVersion = HytaleVersionExtractor.getHytaleVersion(userHytaleJar) + "-user";
            outputJar = serverJarOutput(hytaleVersion, patchline, false);
            assetsZip = assetsZipOutput(hytaleVersion, patchline);
            var assetsJar = assetsZipOutput(hytaleVersion, patchline, "jar");

            try {
                Files.writeString(getServerDir().resolve(".version"), hytaleVersion);
                Files.createSymbolicLink(outputJar, userHytaleJar);
                Files.createSymbolicLink(assetsZip, userAssetsZip);
                Files.createSymbolicLink(assetsJar, userAssetsZip);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return;
        } else downloadHytale(hytaleExtension);

        try {
            Files.writeString(getServerDir().resolve(".patchline"), hytaleExtension.patchLine.get());
            Files.writeString(timePath, dateTimeFormatter.format(LocalDateTime.now()));
            Files.createSymbolicLink(assetsZipOutput(hytaleVersion, hytaleExtension.patchLine.get(), "jar"), assetsZip);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void downloadHytale(HytaleExtension hytaleExtension) {
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
            while ((b = is.read()) != -1) {
                System.out.write(b);
                if (b == '\n' || b == '\r') System.out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println();

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
    }

    private static Path serverJarOutput(String version, String patchline, Boolean stripped) {
        version = patchline + "." + version;

        if (stripped) {
            version += "-stripped";
        }

        return getServerDir().resolve("hytale-server-" + version + ".jar");
    }

    private static Path assetsZipOutput(String version, String patchline) {
        return assetsZipOutput(version, patchline, "zip");
    }

    private static Path assetsZipOutput(String version, String patchline, String fileExt) {
        version = patchline + "." + version;

        return getServerDir().resolve("hytale-server-assets-" + version + "." + fileExt);
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

    public record Status(Boolean upToDate, String version, String patchline, Path output) {
    }

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

        if (hytaleExtension.useUserHytaleJar.get() && !version.contains("user")) {
            version += "-user";
        }

        if (!hytaleExtension.useUserHytaleJar.get()) {
            version = version.replace("-user", "");
        }

        var selected = serverJarOutput(version, patchline, hytaleExtension.stripHytaleJar.get());
        var selectedAssets = assetsZipOutput(version, patchline);

        LocalDateTime time;
        try {
            var timeStr = Files.readString(timePath);
            time = LocalDateTime.parse(timeStr, dateTimeFormatter);
        } catch (IOException e) {
            time = LocalDateTime.MIN;
        }

        var now = LocalDateTime.now();
        boolean upToDate = Duration.between(now, time).compareTo(hytaleExtension.serverLifetime.get()) < 0;

        return new Status(
                hytaleExtension.useUserHytaleJar.get() || (Files.exists(selected) && Files.exists(selectedAssets) && upToDate),
                version,
                patchline,
                selected
        );
    }

    public static Path getOutputDirectory() {
        return outputDirectory;
    }

    public static Path getOutputJar() {
        return outputJar;
    }

//    public static Path getOutputForceStrippedJar() {
//        return outputJar.getFileName().toString().contains("stripped")
//                ? outputJar
//                : getServerDir().resolve(outputJar.getFileName().toString().replace(".jar", "-stripped.jar"));
//    }

    public static Path getServerDir() {
        return serverDirectory;
    }

    public static Path getAssetsZip() {
        return assetsZip;
    }
}
