package com.example.storageutil.config;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ResourceUtils;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.IOException;
import java.util.Map;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = AbstractIntegrationTestConfiguration.Initializer.class)
@SuppressWarnings("checkstyle:MagicNumber")
public abstract class AbstractIntegrationTestConfiguration {

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        private DockerComposeContainer<?> minioContainer = new DockerComposeContainer(ResourceUtils.getFile("classpath:./docker-compose.yml"))
                .withEnv("MINIO_ROOT_USER", "admin")
                .withEnv("MINIO_ROOT_PASSWORD", "password");

        public Initializer() throws IOException {
        }

        public Map<String, Object> getProperties() {
            minioContainer.start();
            return Map.of(
                    "minio.url", "http://127.0.0.1",
                    "minio.port", 9006,
                    "minio.user", "admin",
                    "minio.password", "password");
        }

        @Override
        public void initialize(ConfigurableApplicationContext context) {
            var env = context.getEnvironment();
            env.getPropertySources().addFirst(new MapPropertySource("testcontainers", getProperties()));
        }
    }
}
