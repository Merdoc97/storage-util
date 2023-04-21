package com.example.storageutil;

import com.example.storageutil.config.AbstractIntegrationTestConfiguration;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.imgscalr.Scalr;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "storage.minio.enabled=true",
        "storage,minio.bucket=test-bucket"})
@SuppressWarnings({"checkstyle:MagicNumber", "checkstyle:Indentation"})
class StorageUtilApplicationTests extends AbstractIntegrationTestConfiguration {

    @Autowired
    private StorageService storageService;
    private String userName = "testUser";
    private String pathToStore = "/test/images";
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
        var is = new FileInputStream(testFile);
        var metaData = Map.of("version", "1");
        storageService.uploadFile(userName, pathToStore, fileName,
                "application/png", Map.of("version", "1"), new FileInputStream(testFile), true);
        var message = Assertions.assertThrows(IllegalArgumentException.class, () -> storageService.uploadFile(userName, pathToStore, fileName,
                "application/png", metaData, is, false));
        assertThat(message.getMessage()).isEqualTo(
                String.format("File with path store %s and with file name %s already present and allowToOverride param is false",
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

    @Test
    void deleteFileTest() throws FileNotFoundException {
        assertThat(storageService.isFilePresent(userName, pathToStore, fileName)).isFalse();
        var response = storageService.uploadFile(userName, pathToStore, fileName,
                "application/png", Map.of("application-version", "1"), new FileInputStream(testFile), true);
        assertThat(storageService.isFilePresent(userName, pathToStore, fileName)).isTrue();
        storageService.deleteFile(userName, pathToStore, fileName);
        assertThat(storageService.isFilePresent(userName, pathToStore, fileName)).isFalse();
    }

    @Test
    void uploadFileFromUrl() throws IOException {
        var imageUrl =
                "https://cdn.nba.com/manage/2023/04/draymond-game2-scaled.jpg";

        var bfImage = ImageIO.read(new URL(imageUrl).openStream());
        var resized  = Scalr.resize(bfImage, Scalr.Method.SPEED, bfImage.getWidth(), bfImage.getHeight());

        var imgTmp = imageUrl.split("\\?")[0].split("\\.");

        var fileExtensions = imgTmp[imgTmp.length - 1];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, fileExtensions, baos);
        byte[] bytes = baos.toByteArray();

        assertThat(storageService.isFilePresent(userName, pathToStore, fileName)).isFalse();
        var response = storageService.uploadFile(userName, pathToStore, UUID.randomUUID() + "." + fileExtensions, "application/" + fileExtensions, Map.of(),
                new BufferedInputStream(new ByteArrayInputStream(bytes)), true);
        assertThat(response).isNotNull();
        assertThat(response.getObjectPath()).isNotNull();
    }


}
