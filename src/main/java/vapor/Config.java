package vapor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private final Map<String, String> fileValues = new HashMap<>();

    public static Config load() throws IOException {
        Config config = new Config();
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            return config;
        }
        List<String> lines = Files.readAllLines(envFile);
        for (String line : lines) {
            config.readLine(line.trim());
        }
        return config;
    }

    private void readLine(String line) {
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }
        int separator = line.indexOf('=');
        if (separator < 1) {
            return;
        }
        String name = line.substring(0, separator).trim();
        String value = line.substring(separator + 1).trim();
        fileValues.put(name, value);
    }

    public String get(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return fileValues.getOrDefault(name, defaultValue);
    }

    public String require(String name) {
        String value = get(name, "");
        if (value.isEmpty()) {
            throw new IllegalStateException("Configuração ausente: " + name);
        }
        return value;
    }

    public int getInt(String name, int defaultValue) {
        return Integer.parseInt(get(name, String.valueOf(defaultValue)));
    }
}
