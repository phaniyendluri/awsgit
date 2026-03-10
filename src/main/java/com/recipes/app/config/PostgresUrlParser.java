package com.recipes.app.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class PostgresUrlParser {

    private PostgresUrlParser() {
    }

    public static ParsedPostgresConfig parse(String rawUrl, String fallbackUser, String fallbackPassword) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return new ParsedPostgresConfig("jdbc:postgresql://localhost:5432/recipes", fallbackUser, fallbackPassword);
        }

        if (rawUrl.startsWith("jdbc:postgresql://")) {
            return new ParsedPostgresConfig(rawUrl, fallbackUser, fallbackPassword);
        }

        if (!rawUrl.startsWith("postgresql://")) {
            return new ParsedPostgresConfig(rawUrl, fallbackUser, fallbackPassword);
        }

        URI uri = URI.create(rawUrl);
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                + (uri.getRawPath() == null ? "" : uri.getRawPath())
                + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");

        String username = fallbackUser;
        String password = fallbackPassword;

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            if (parts.length > 1) {
                password = decode(parts[1]);
            }
        }

        return new ParsedPostgresConfig(jdbcUrl, username, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public record ParsedPostgresConfig(String jdbcUrl, String username, String password) {
    }
}
