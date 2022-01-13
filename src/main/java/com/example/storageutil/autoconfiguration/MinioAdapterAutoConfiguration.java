package com.example.storageutil.autoconfiguration;

import com.example.storageutil.StorageService;
import com.example.storageutil.client.minio.MinIoStorageServiceImpl;
import com.example.storageutil.config.MinioAdapterConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "storage.minio", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(value = MinioAdapterConfigProperties.class)
@RequiredArgsConstructor
public class MinioAdapterAutoConfiguration {

    private final MinioAdapterConfigProperties minioProperties;

    @Bean
    @ConditionalOnMissingBean
    public StorageService storageService() {
        return new MinIoStorageServiceImpl(minioProperties);
    }
}
