package com.example.storageutil.client.minio;

import com.example.storageutil.StorageService;
import com.example.storageutil.config.MinioAdapterConfigProperties;
import com.example.storageutil.dto.DownloadObjectResponse;
import com.example.storageutil.dto.UploadFileResponse;
import com.example.storageutil.exceptions.FileNotFoundException;
import com.example.storageutil.util.PathUtil;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;

import java.io.InputStream;
import java.util.Map;

@Slf4j
public class MinIoStorageServiceImpl implements StorageService, InitializingBean {
    private static final String PUBLIC_POLICY_CONFIGURATION =
            "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetBucketLocation\",\"s3:ListBucket\",\"s3:ListBucketMultipartUploads\"],\"Resource\":[\"arn:aws:s3:::test-bucket\"]},{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:AbortMultipartUpload\",\"s3:DeleteObject\",\"s3:GetObject\",\"s3:ListMultipartUploadParts\",\"s3:PutObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}";
    private final Integer minioPort;
    private final String accessKey;
    private final String secretKey;
    private final String bucket;
    private final MinioClient minioClient;
    private final MinioUploadOperations uploadOperations;
    private final int fileSize;
    private final long maxMultipartSize;
    private final MinioAdapterConfigProperties properties;
    public MinIoStorageServiceImpl(final MinioAdapterConfigProperties minioProperties) {
        this.minioPort = minioProperties.getPort();
        this.accessKey = minioProperties.getUser();
        this.secretKey = minioProperties.getPassword();
        this.bucket = minioProperties.getBucket();
        this.minioClient = MinioClient.builder()
                .credentials(accessKey, secretKey)
                .endpoint(minioProperties.getUrl(), minioPort, minioProperties.isSecure())
                .build();
        this.fileSize = minioProperties.getFileSize();
        this.maxMultipartSize = minioProperties.getMaxMultipartSize();
        this.uploadOperations = MinioUploadOperations.builder()
                .minioClient(this.minioClient)
                .bucket(this.bucket)
                .fileSize(this.fileSize)
                .maxMultipartSize(this.maxMultipartSize)
                .build();
        this.properties=minioProperties;
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

    @Override
    @SneakyThrows
    public void deleteFile(String username, String path, String fileName) {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(PathUtil.buildFullPath(username, path, fileName))
                .build());
    }

    @SneakyThrows
    @Override
    public void afterPropertiesSet() {
        log.info("Validate Storage implementation input params");
        if (!this.minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket).build())) {
            log.info("Create bucket which not exist: {}", this.bucket);
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(this.bucket)
                    .build());
            if (properties.isBucketPublic()) {
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(String.format(PUBLIC_POLICY_CONFIGURATION, bucket))
                        .build());
            }
        }
    }
}
