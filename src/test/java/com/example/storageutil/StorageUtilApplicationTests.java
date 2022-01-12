package com.example.storageutil;

import com.example.storageutil.config.AbstractIntegrationTestConfiguration;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
class StorageUtilApplicationTests extends AbstractIntegrationTestConfiguration {

    @Autowired
    private StorageService storageService;
    private String userName = "testUser";
    private String pathToStore = "/images";
    private String fileName;
    private File testFile = new File(this.getClass().getResource("/test.png").getFile());
    private String headerPrefix = "x-amz-meta-";

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
        storageService.uploadFile(userName, pathToStore, fileName,
                "application/png", Map.of("version", "1"), new FileInputStream(testFile), true);
        var message = Assertions.assertThrows(IllegalArgumentException.class, () -> storageService.uploadFile(userName, pathToStore, fileName,
                        "application/png", Map.of("version", "1"), new FileInputStream(testFile), false))
                .getMessage();
        assertThat(message).isEqualTo(String.format("File with path store %s and with file name %s already present and allowToOverride param is false",
                pathToStore, fileName));
    }

    @Test
    void testDownloadOperations() throws IOException {
        assertThat(storageService.isFilePresent(userName, pathToStore, fileName)).isFalse();
        var response = storageService.uploadFile(userName, pathToStore, fileName,
                "application/png", Map.of("application-version", "1"), new FileInputStream(testFile), true);
        assertThat(storageService.isFilePresent(userName, pathToStore, fileName)).isTrue();
        var fileToSave = new File(testFile.getParentFile() + "/copy-" + fileName);
        if (!fileToSave.exists()) {
            fileToSave.createNewFile();
        }
        var is = storageService.downloadFile(userName, pathToStore, fileName);
        IOUtils.copy(is.getInputStream(), new FileOutputStream(fileToSave));
        IOUtils.closeQuietly(is.getInputStream());
        var initFile = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
        var savedFile = new String(Files.readAllBytes(fileToSave.toPath()), StandardCharsets.UTF_8);
        assertThat(savedFile).isEqualTo(initFile);
        assertThat(is.getHeaders().get(headerPrefix + "application-version")).isEqualTo("1");
        assertThat(is.getHeaders().get("content-type")).isEqualTo("application/png");
    }

}
