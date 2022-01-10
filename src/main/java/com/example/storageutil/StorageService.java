package com.example.storageutil;

import com.example.storageutil.dto.UploadFileResponse;

import java.io.InputStream;
import java.util.Map;

public interface StorageService {

    UploadFileResponse uploadFile(String username, String pathToStore, String fileName, String contentType,
                                  Map<String, String> metadata, InputStream inputStream, boolean allowToOverride);

    boolean isFilePresent(String username, String pathToStore, String fileName);
}
