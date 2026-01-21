package net.wiredtomato.hygradle.version;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Version {
    public static final String VERSION;

    static {
        try (var resource = Version.class.getResource("version").openStream()) {
            var data = resource.readAllBytes();

            VERSION = new String(data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Version() {
    }
}
