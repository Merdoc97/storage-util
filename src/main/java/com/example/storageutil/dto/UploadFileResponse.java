package com.example.storageutil.dto;

import lombok.Value;

@Value(staticConstructor = "of")
public class UploadFileResponse {

    private String fileName;
    private String objectPath;

}
