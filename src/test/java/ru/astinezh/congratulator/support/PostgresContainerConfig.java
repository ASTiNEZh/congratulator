package ru.astinezh.congratulator.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL в Docker для интеграционных тестов.
 * {@code @ServiceConnection} сам подставляет url/логин/пароль в DataSource,
 * поэтому настройки БД из application.properties тестами не используются.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresContainerConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    }
}
