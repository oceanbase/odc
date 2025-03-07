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
package com.oceanbase.odc.service.exporter.impl;

import static com.oceanbase.odc.service.exporter.model.ExportConstants.HMAC_ALGORITHM;
import static com.oceanbase.odc.service.exporter.utils.JsonExtractorFactory.getConfigJson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.LinkedHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.exporter.Extractor;
import com.oceanbase.odc.service.exporter.model.Encryptable;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportRowDataReader;
import com.oceanbase.odc.service.exporter.model.ExportedFile;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class JsonExtractor implements Extractor<JsonNode> {

    private String tempFilePath;
    private ExportedFile exportedFile;

    public JsonExtractor() {}


    @Override
    public boolean checkSignature() {
        if (exportedFile.getSecret() == null) {
            return true;
        }
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper objectMapper = new ObjectMapper();
        File configJson = getConfigJson(this.tempFilePath);
        Verify.notNull(configJson, "Invalid archived file");
        try (InputStream inputStream = Files.newInputStream(configJson.toPath())) {
            JsonParser jsonParser = jsonFactory.createParser(inputStream);

            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(exportedFile.getSecret().getBytes(), HMAC_ALGORITHM);
            mac.init(secretKeySpec);

            // Start reading the JSON structure
            JsonToken jsonToken = jsonParser.nextToken();
            if (jsonToken != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected data to start with an Object");
            }

            String signature = null;
            LinkedHashMap<String, Object> metadata = null;

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jsonParser.getCurrentName();
                jsonToken = jsonParser.nextToken(); // Move to the value of the field

                switch (fieldName) {
                    case "signature":
                        signature = jsonParser.getValueAsString();
                        break;
                    case "metadata":
                        metadata = objectMapper.readValue(jsonParser,
                                new TypeReference<LinkedHashMap<String, Object>>() {});
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
                log.info("Invalid signature,  archivedFile={},computedSignature={},signature={}", exportedFile,
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


    public ExportRowDataReader<JsonNode> getRowDataReader() throws IOException {
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


            LinkedHashMap<String, Object> metadata = objectMapper.readValue(jsonParser,
                    new TypeReference<LinkedHashMap<String, Object>>() {});

            ExportProperties properties = new ExportProperties(metadata, null);
            properties.putTransientProperties("signature", getSignature());
            jsonParser.nextToken(); // Move to next field after metadata
            if (!"data".equals(jsonParser.getCurrentName())) {
                throw new IllegalStateException("Expected next field to be 'data'");
            }

            jsonToken = jsonParser.nextToken(); // Move to data array
            if (jsonToken != JsonToken.START_ARRAY) {
                throw new IllegalStateException("Expected data to be an Array");
            }
            return new JsonRowDataReader(properties, jsonParser, objectMapper, exportedFile.getSecret(), tempFilePath);
        }
    }

    private String getSignature() {
        JsonFactory jsonFactory = new JsonFactory();
        File configJson = getConfigJson(this.tempFilePath);
        try (InputStream inputStream = Files.newInputStream(configJson.toPath())) {
            JsonParser jsonParser = jsonFactory.createParser(inputStream);
            JsonToken jsonToken = jsonParser.nextToken();
            if (jsonToken != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected data to start with an Object");
            }
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = jsonParser.getCurrentName();
                if ("signature".equals(fieldName)) {
                    jsonParser.nextToken();
                    return jsonParser.getText();
                } else {
                    jsonParser.nextToken();
                    jsonParser.skipChildren();
                }
            }
            throw new IllegalStateException("Missing required fields in JSON data");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private final static class JsonRowDataReader implements ExportRowDataReader<JsonNode> {

        private final ExportProperties metadata;
        private final JsonParser jsonParser;
        private final ObjectMapper objectMapper;
        private final String encryptKey;
        private final String tempFilePath;
        private Integer rowCount = 0;

        public JsonRowDataReader(ExportProperties metadata, JsonParser jsonParser, ObjectMapper objectMapper,
                String encryptKey, String tempFilePath) {
            this.metadata = metadata;
            this.jsonParser = jsonParser;
            this.objectMapper = objectMapper;
            this.encryptKey = encryptKey;
            this.tempFilePath = tempFilePath;
        }

        @Override
        public ExportProperties getProperties() {
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
