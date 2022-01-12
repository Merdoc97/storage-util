package com.example.storageutil.dto;

import lombok.Value;
import okhttp3.Headers;

import java.io.InputStream;

@Value(staticConstructor = "of")
public class DownloadObjectResponse {
    private InputStream inputStream;
    private Headers headers;
}
