package com.example.storageutil;

import com.example.storageutil.client.minio.MinIoStorageServiceImpl;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class StorageUtilApplicationTests {

    private MinIoStorageServiceImpl storageService = new MinIoStorageServiceImpl("http://localhost", 9002, false,
            "admin", "password", "test-bucket", null, null);
    private String userName = "testUser";
    private String pathToStore = "/images";
    private String fileName;
    private File testFile = new File(this.getClass().getResource("/test.png").getFile());

    @BeforeEach
    void setUp() {
        this.fileName = "test" + new Random().nextInt() + ".png";
    }

    @Test
    void uploadFileTestPositive() throws FileNotFoundException {
        assertThat(storageService.isFilePresent(userName, pathToStore, fileName)).isFalse();
        var response = storageService.uploadFile(userName, pathToStore, fileName,
                "application/png", Map.of("version", "1"), new FileInputStream(testFile), true);
        assertThat(response).isNotNull();
        assertThat(response.getObjectPath()).isEqualTo("/" +
                userName + "/"
                + pathToStore.replaceAll("/", Strings.EMPTY)
                + "/"
                + fileName);
        assertThat(response.getFileName()).isEqualTo(fileName);
        assertThat(storageService.isFilePresent(userName, pathToStore, fileName)).isTrue();
    }

    @Test
    void uploadFileIfOverrideNotAllowed() throws FileNotFoundException {
        //create file for current test
        var response = storageService.uploadFile(userName, pathToStore, fileName,
                "application/png", Map.of("version", "1"), new FileInputStream(testFile), true);
        var message = Assertions.assertThrows(IllegalArgumentException.class, () -> storageService.uploadFile(userName, pathToStore, fileName,
                        "application/png", Map.of("version", "1"), new FileInputStream(testFile), false))
                .getMessage();
        assertThat(message).isEqualTo(String.format("File with path store %s and with file name %s already present and allowToOverride param is false",
                pathToStore, fileName));
    }

}
