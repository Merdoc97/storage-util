package com.example.storageutil.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static io.minio.ObjectWriteArgs.MIN_MULTIPART_SIZE;

@Getter
@Setter
@ConfigurationProperties(prefix = "storage.minio")
@Validated
public class MinioAdapterConfigProperties {

    @NotBlank(message = "url property is required")
    private String url;

    @NotNull(message = "port property is required")
    private Integer port;

    @NotBlank(message = "user property is required")
    private String user;

    @NotBlank(message = "password property is required")
    private String password;

    @NotBlank(message = "bucket property is required")
    private String bucket;

    @NotNull(message = "fileSize property is required")
    private Integer fileSize = -1;

    @NotNull(message = "maxMultipartSize property is required")
    private Long maxMultipartSize = (long) MIN_MULTIPART_SIZE;

    private boolean secure;
}
