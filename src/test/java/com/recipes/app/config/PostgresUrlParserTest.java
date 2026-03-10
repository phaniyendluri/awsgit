package com.recipes.app.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostgresUrlParserTest {

    @Test
    void parsesNeonStylePostgresUrlAndExtractsCredentials() {
        String rawUrl = "postgresql://neondb_owner:secret123@ep-super-feather.ak6irl2k.neon.tech/neondb?sslmode=require&channel_binding=require";

        PostgresUrlParser.ParsedPostgresConfig parsed =
                PostgresUrlParser.parse(rawUrl, "fallbackUser", "fallbackPassword");

        assertEquals("jdbc:postgresql://ep-super-feather.ak6irl2k.neon.tech/neondb?sslmode=require&channel_binding=require", parsed.jdbcUrl());
        assertEquals("neondb_owner", parsed.username());
        assertEquals("secret123", parsed.password());
    }

    @Test
    void keepsJdbcUrlAsIsAndUsesFallbackCredentials() {
        String rawUrl = "jdbc:postgresql://localhost:5432/recipes";

        PostgresUrlParser.ParsedPostgresConfig parsed =
                PostgresUrlParser.parse(rawUrl, "postgres", "postgres");

        assertEquals(rawUrl, parsed.jdbcUrl());
        assertEquals("postgres", parsed.username());
        assertEquals("postgres", parsed.password());
    }
}
