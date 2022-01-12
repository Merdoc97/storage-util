package com.example.storageutil.config;

import com.example.storageutil.StorageService;
import com.example.storageutil.client.minio.MinIoStorageServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfiguration {

    @Bean
    public StorageService storageService(@Value("${minio.url}") String url,
                                         @Value("${minio.port}") Integer port,
                                         @Value("${minio.user}") String user,
                                         @Value("${minio.password}") String password,
                                         @Value("${minio.bucket}") String bucket) {
        return new MinIoStorageServiceImpl(url, port, false,
                user, password, bucket, null, null);
    }
}
