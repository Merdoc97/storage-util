package com.example.storageutil.config;

import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = AbstractIntegrationTestConfiguration.Initializer.class)
@SuppressWarnings("checkstyle:MagicNumber")
public abstract class AbstractIntegrationTestConfiguration {

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        private GenericContainer<?> minioContainer =
                new GenericContainer<>("docker.io/bitnami/minio:2022")
                        .withEnv("MINIO_ROOT_USER", "admin")
                        .withEnv("MINIO_ROOT_PASSWORD", "password")
                        .withExposedPorts(9000)
                        .withStartupAttempts(3);

        public Initializer() throws IOException, URISyntaxException, InterruptedException {
        }

        public Map<String, Object> getProperties() throws InterruptedException {
            minioContainer.start();
            return Map.of(
                    "minio.url", "http://" + minioContainer.getContainerIpAddress(),
                    "minio.port", minioContainer.getMappedPort(9000),
                    "minio.user", "admin",
                    "minio.password", "password");
        }

        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            var env = context.getEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("testcontainers", getProperties()));
        }
    }
}
