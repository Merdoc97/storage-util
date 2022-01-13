package com.example.storageutil.client.minio;

import com.example.storageutil.StorageService;
import com.example.storageutil.dto.DownloadObjectResponse;
import com.example.storageutil.dto.UploadFileResponse;
import com.example.storageutil.exceptions.FileNotFoundException;
import com.example.storageutil.util.PathUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.util.Map;

import static io.minio.ObjectWriteArgs.MIN_MULTIPART_SIZE;

@Slf4j
public class MinIoStorageServiceImpl implements StorageService, InitializingBean {

    private final String minioUrl;
    private final Integer minioPort;
    private final String accessKey;
    private final String secretKey;
    private final String bucket;
    private final MinioClient minioClient;
    private final MinioUploadOperations uploadOperations;
    private final int fileSize;
    private final long maxMultipartSize;

    public MinIoStorageServiceImpl(final String minioUrl, final Integer minioPort,
                                   final boolean secure, final String accessKey,
                                   final String secretKey, final String bucket,
                                   final Integer fileSize, final Long maxMultipartSize) {
        this.minioUrl = minioUrl;
        this.minioPort = minioPort;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.bucket = bucket;
        this.minioClient = MinioClient.builder()
                .credentials(accessKey, secretKey)
                .endpoint(minioUrl, minioPort, secure)
                .build();
        this.fileSize = fileSize == null ? -1 : fileSize;
        this.maxMultipartSize = maxMultipartSize == null ? MIN_MULTIPART_SIZE : maxMultipartSize;
        this.uploadOperations = MinioUploadOperations.builder()
                .minioClient(this.minioClient)
                .bucket(this.bucket)
                .fileSize(this.fileSize)
                .maxMultipartSize(this.maxMultipartSize)
                .build();
    }

    @Override
    public UploadFileResponse uploadFile(String username, String pathToStore, String fileName,
                                         String contentType, Map<String, String> metadata,
                                         InputStream inputStream, boolean allowToOverride) {
        return uploadOperations.uploadFile(username, pathToStore, fileName, contentType, metadata, inputStream, allowToOverride);
    }

    @Override
    public boolean isFilePresent(String username, String pathToStore, String fileName) {
        return uploadOperations.isFilePresent(PathUtil.buildFullPath(username, pathToStore, fileName));
    }

    @Override
    @SneakyThrows
    public DownloadObjectResponse downloadFile(String username, String path, String fileName) {
        if (!isFilePresent(username, path, fileName)) {
            throw new FileNotFoundException("Searching file %s not found");
        }

        var minioResponse = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(PathUtil.buildFullPath(username, path, fileName))
                .build());
        return DownloadObjectResponse.of(minioResponse, minioResponse.headers());

    }

    @SneakyThrows
    @Override
    public void afterPropertiesSet() {
        log.info("Validate Storage implementation input params");
        if (minioPort == null) {
            throw new IllegalArgumentException("Storage port is null please verify configuration");
        }
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("Storage secret key is null or empty");
        }
        if (accessKey == null || accessKey.isEmpty()) {
            throw new IllegalArgumentException("Storage access key is null or empty");
        }
        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalArgumentException("Storage bucket is empty or null");
        }
        if (!this.minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket).build())) {
            log.info("Create bucket which not exist: {}", this.bucket);
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(this.bucket)
                    .build());
        }
    }
}
