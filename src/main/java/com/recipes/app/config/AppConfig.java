package com.recipes.app.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.time.Duration;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
    }

    @Bean
    public DataSource dataSource(Environment environment) {
        String rawUrl = environment.getProperty("POSTGRES_URL", "jdbc:postgresql://localhost:5432/recipes");
        String fallbackUser = environment.getProperty("POSTGRES_USER", "postgres");
        String fallbackPassword = environment.getProperty("POSTGRES_PASSWORD", "postgres");

        PostgresUrlParser.ParsedPostgresConfig parsed = PostgresUrlParser.parse(rawUrl, fallbackUser, fallbackPassword);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(parsed.jdbcUrl());
        dataSource.setUsername(parsed.username());
        dataSource.setPassword(parsed.password());
        return dataSource;
    }
}
