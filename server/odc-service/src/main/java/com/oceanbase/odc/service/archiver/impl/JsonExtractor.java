/*
 * Copyright (c) 2023 OceanBase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oceanbase.odc.service.archiver.impl;

import static com.oceanbase.odc.service.archiver.model.ArchiveConstants.HMAC_ALGORITHM;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.EncryptAlgorithm;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.archiver.Extractor;
import com.oceanbase.odc.service.archiver.model.ArchiveProperties;
import com.oceanbase.odc.service.archiver.model.ArchiveRowDataReader;
import com.oceanbase.odc.service.archiver.model.ArchivedData;
import com.oceanbase.odc.service.archiver.model.ArchivedFile;
import com.oceanbase.odc.service.archiver.model.Encryptable;
import com.oceanbase.odc.service.common.util.OdcFileUtil;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonExtractor implements Extractor<JsonNode> {

    String tempFilePath;
    @Getter
    ArchivedFile archivedFile;

    private JsonExtractor() {}

    @SneakyThrows
    public static JsonExtractor buildJsonExtractor(ArchivedFile archivedFile, String tempPath) {
        JsonExtractor jsonExtractor = new JsonExtractor();
        // Create a random directory within the specified destination path
        Path randomDir = Files.createTempDirectory(new File(tempPath).toPath(), "unzipped-");

        // Create a temporary file to save the InputStream contents
        File tempZipFile = File.createTempFile("tempZip", ".zip", randomDir.toFile());
        tempZipFile.deleteOnExit();
        jsonExtractor.tempFilePath = randomDir.toFile().getPath();

        // Write the InputStream to the temporary zip file
        try (FileOutputStream fos = new FileOutputStream(tempZipFile);
                InputStream inputStream = archivedFile.getProvider().getInputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
        // Unzip the temporary file into the random directory
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tempZipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = randomDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    if (entryPath.getParent() != null) {
                        Files.createDirectories(entryPath.getParent());
                    }
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
        log.info("Files extracted to: {}", randomDir.toAbsolutePath());
        File file = MoreObjects.firstNonNull(getConfigTxt(jsonExtractor.tempFilePath),
                getConfigJson(jsonExtractor.tempFilePath));
        jsonExtractor.archivedFile = ArchivedFile.fromFile(file, archivedFile.getSecret());
        return jsonExtractor;
    }

    public static File getConfigTxt(String path) {
        File configTxt = new File(path, "config.txt");
        if (configTxt.exists() && configTxt.isFile()) {
            return configTxt;
        } else {
            return null;
        }
    }

    public static File getConfigJson(String path) {
        File configJson = new File(path, "config.json");

        if (configJson.exists() && configJson.isFile()) {
            return configJson;
        } else {
            return null;
        }
    }

    @Override
    public boolean checkSignature() {
        if (archivedFile.getSecret() == null || !archivedFile.isCheckConfigJsonSignature()) {
            return true;
        }
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper objectMapper = new ObjectMapper();
        File configJson = getConfigJson(this.tempFilePath);
        Verify.notNull(configJson, "Invalid archived file");
        try (InputStream inputStream = Files.newInputStream(configJson.toPath())) {
            JsonParser jsonParser = jsonFactory.createParser(inputStream);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(archivedFile.getSecret().getBytes(), HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            // Start reading the JSON structure
            JsonToken jsonToken = jsonParser.nextToken();
            if (jsonToken != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected data to start with an Object");
            }

            String signature = null;
            ArchiveProperties metadata = null;

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jsonParser.getCurrentName();
                jsonToken = jsonParser.nextToken(); // Move to the value of the field

                switch (fieldName) {
                    case "signature":
                        signature = jsonParser.getValueAsString();
                        break;
                    case "metadata":
                        metadata = objectMapper.readValue(jsonParser, ArchiveProperties.class);
                        mac.update(
                                ("metadata" + objectMapper.writeValueAsString(metadata))
                                        .getBytes(StandardCharsets.UTF_8));
                        break;
                    case "data":
                        if (jsonToken != JsonToken.START_ARRAY) {
                            throw new IllegalStateException("Expected data to be an Array");
                        }
                        // Process data array items
                        while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                            // Read each item as JSON and update mac
                            String rowDataJson = objectMapper.writeValueAsString(objectMapper.readTree(jsonParser));
                            mac.update(rowDataJson.getBytes());
                        }
                        break;
                }
            }

            if (signature == null || metadata == null) {
                throw new IllegalStateException("Missing required fields in JSON data");
            }

            // Generate checksum
            String computedSignature = Base64.getEncoder().encodeToString(mac.doFinal());

            // Verify signature
            if (!computedSignature.equals(signature)) {
                log.info("Invalid signature,  archivedFile={},computedSignature={},signature={}", archivedFile,
                        computedSignature, signature);
                return false;
            }
            return true;

        } catch (Exception e) {
            log.warn("Error in signature verification: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        OdcFileUtil.deleteFiles(new File(tempFilePath));
    }

    public <D> ArchivedData<D> extractFullData(TypeReference<ArchivedData<D>> typeReference)
            throws Exception {
        try (InputStream inputStream = archivedFile.getProvider().getInputStream()) {
            String decryptedString = decrypt(inputStream, archivedFile.getSecret());
            return JsonUtils.fromJson(decryptedString, typeReference);
        }
    }

    public ArchiveRowDataReader<JsonNode> getRowDataReader() throws Exception {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
        File configJson = getConfigJson(this.tempFilePath);
        Verify.notNull(configJson, "Invalid archived file");
        try (InputStream inputStream = Files.newInputStream(configJson.toPath())) {
            JsonParser jsonParser = jsonFactory.createParser(inputStream);

            JsonToken jsonToken = jsonParser.nextToken();
            if (jsonToken != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected data to start with an Object");
            }

            jsonParser.nextToken(); // Move to "metadata" field
            if (!"metadata".equals(jsonParser.getCurrentName())) {
                throw new IllegalStateException("Expected first field to be 'metadata'");
            }

            jsonToken = jsonParser.nextToken(); // Move to metadata value
            if (jsonToken != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected metadata to be an Object");
            }

            ArchiveProperties metadata = objectMapper.readValue(jsonParser, ArchiveProperties.class);

            jsonParser.nextToken(); // Move to next field after metadata
            if (!"data".equals(jsonParser.getCurrentName())) {
                throw new IllegalStateException("Expected next field to be 'data'");
            }

            jsonToken = jsonParser.nextToken(); // Move to data array
            if (jsonToken != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected data to be an Array");
            }
            return new JsonRowDataReader(metadata, jsonParser, objectMapper, archivedFile.getSecret(), tempFilePath);
        }
    }

    private String decrypt(InputStream inputStream, @Nullable String key) throws Exception {
        String json = convertInputStreamToString(inputStream);
        if (key == null) {
            return json;
        }
        return EncryptAlgorithm.AES.decrypt(json, key, StandardCharsets.UTF_8.name());
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        char[] buffer = new char[1024];
        int bytesRead;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            while ((bytesRead = reader.read(buffer, 0, buffer.length)) != -1) {
                stringBuilder.append(buffer, 0, bytesRead);
            }
        }

        return stringBuilder.toString();
    }

    public static class JsonRowDataReader implements ArchiveRowDataReader<JsonNode> {

        private final ArchiveProperties metadata;
        private final JsonParser jsonParser;
        private final ObjectMapper objectMapper;
        private final String encryptKey;
        private final String tempFilePath;
        private Integer rowCount = 0;

        public JsonRowDataReader(ArchiveProperties metadata, JsonParser jsonParser, ObjectMapper objectMapper,
                String encryptKey, String tempFilePath) {
            this.metadata = metadata;
            this.jsonParser = jsonParser;
            this.objectMapper = objectMapper;
            this.encryptKey = encryptKey;
            this.tempFilePath = tempFilePath;
        }

        @Override
        public ArchiveProperties getMetaData() {
            return metadata;
        }

        @Override
        public <R extends Encryptable> R readRow(Class<R> rowDataClass) throws IOException {
            JsonNode jsonNode = readRow();
            R rowData = objectMapper.convertValue(jsonNode, rowDataClass);
            if (rowData != null && encryptKey != null) {
                rowData.decrypt(encryptKey);
            }
            return rowData;
        }

        @Override
        public JsonNode readRow() throws IOException {
            JsonToken jsonToken = jsonParser.nextToken();
            if (jsonToken == JsonToken.END_ARRAY) {
                return null;
            }
            JsonNode rowDataNode = jsonParser.readValueAsTree();
            rowCount++;
            return rowDataNode;
        }

        @Override
        public File getFile(String fileName) {
            return new File(this.tempFilePath, fileName);
        }

        @Override
        public Integer getRowNumber() {
            return rowCount;
        }

        @Override
        public void close() throws IOException {
            jsonParser.close();
        }
    }
}
