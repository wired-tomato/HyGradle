package net.wiredtomato.hygradle.hytale;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

public class HytaleVersionExtractor {
    public static String getHytaleVersion(Path hytaleServerJar) {
        try {
            return getHytaleVersion(new ZipFile(hytaleServerJar.toFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getHytaleVersion(ZipFile hytaleServerZip) {
        try {
            var manifestEntry = hytaleServerZip.getEntry("META-INF/MANIFEST.MF");
            InputStream input = hytaleServerZip.getInputStream(manifestEntry);

            Manifest manifest = new Manifest(input);
            return manifest.getMainAttributes().getValue("Implementation-Version");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
