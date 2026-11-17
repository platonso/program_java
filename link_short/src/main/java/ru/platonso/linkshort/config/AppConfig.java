package ru.platonso.linkshort.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/** Immutable settings loaded from application.properties. */
public record AppConfig(String baseUrl, int defaultClickLimit, int maxClickLimit,
                        Duration ttl, Duration cleanupPeriod) {
    public AppConfig {
        if (baseUrl == null || baseUrl.isBlank() || defaultClickLimit < 1 || maxClickLimit < defaultClickLimit
                || ttl.isNegative() || ttl.isZero() || cleanupPeriod.isNegative() || cleanupPeriod.isZero()) {
            throw new IllegalArgumentException("Некорректная конфигурация приложения");
        }
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) throw new IllegalStateException("Не найден application.properties");
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать конфигурацию", exception);
        }
        return new AppConfig(
                required(properties, "base-url"),
                positive(properties, "default-click-limit"),
                positive(properties, "max-click-limit"),
                Duration.ofHours(positive(properties, "ttl-hours")),
                Duration.ofSeconds(positive(properties, "cleanup-period-seconds")));
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Не задан параметр " + key);
        return value.trim();
    }
    private static int positive(Properties properties, String key) {
        try {
            int value = Integer.parseInt(required(properties, key));
            if (value < 1) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Параметр " + key + " должен быть положительным числом");
        }
    }
}
