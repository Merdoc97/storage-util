package com.example.storageutil.client.minio;

import com.example.storageutil.dto.UploadFileResponse;
import com.example.storageutil.util.PathUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Map;

@AllArgsConstructor
@Builder
@Slf4j
public final class MinioUploadOperations {
    private final MinioClient minioClient;
    private final String bucket;
    private final int fileSize;
    private final long maxMultipartSize;

    public UploadFileResponse uploadFile(String username, String pathToStore, String fileName,
                                         String contentType, Map<String, String> metadata,
                                         InputStream inputStream, boolean allowToOverride) {

        if (!allowToOverride) {
            log.info("uploading file with override param {}", allowToOverride);
            if (isFilePresent(PathUtil.buildFullPath(username, pathToStore, fileName))) {
                throw new IllegalArgumentException(
                        String.format("File with path store %s and with file name %s already present and allowToOverride param is false",
                                pathToStore, fileName));
            }
        }
        return upload(username, pathToStore, fileName, contentType, metadata, inputStream);
    }

    @SneakyThrows
    private UploadFileResponse upload(String username, String pathToStore, String fileName,
                                      String contentType, Map<String, String> metadata,
                                      InputStream inputStream) {
        final var fullPath = PathUtil.buildFullPath(username, pathToStore, fileName);
        log.info("Uploading file to min io server with path {}", fullPath);
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .contentType(contentType)
                .stream(inputStream, fileSize, maxMultipartSize)
                .userMetadata(metadata)
                .object(fullPath)
                .build());
        return UploadFileResponse.of(fileName, fullPath);
    }

    @SneakyThrows
    public boolean isFilePresent(String object) {
        try {
            var response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(object)
                    .build());
            return response.object() != null && !response.object().isEmpty();
        } catch (final ErrorResponseException e) {
            return false;
        }
    }
}
