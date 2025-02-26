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
import static com.oceanbase.odc.service.common.util.OdcFileUtil.createFileWithDirectories;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.EncryptAlgorithm;
import com.oceanbase.odc.service.exporter.Exporter;
import com.oceanbase.odc.service.exporter.model.Encryptable;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportRowDataAppender;
import com.oceanbase.odc.service.exporter.model.ExportedData;
import com.oceanbase.odc.service.exporter.model.ExportedFile;
import com.oceanbase.odc.service.exporter.model.ExportedZipFileFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalJsonExporter implements Exporter {

    @Override
    public ExportedFile archiveFullData(Object data, ExportProperties metaData, String encryptKey)
            throws Exception {

        ExportedData<Object> exportedData = new ExportedData<>();
        exportedData.setMetadata(metaData);
        exportedData.setData(data);
        String json = JsonUtils.toJson(exportedData);
        String fileName = metaData.acquireConfigJsonFileUrl();
        if (encryptKey != null) {
            json = EncryptAlgorithm.AES.encrypt(json, encryptKey, StandardCharsets.UTF_8.name());
            fileName = metaData.acquireConfigTxtFileUrl();
        }
        File configFile = writeToFile(fileName, json);
        ExportedFile exportedFile = new ExportedZipFileFactory(configFile, null)
                .build(metaData.acquireZipFileUrl(), encryptKey);
        exportedFile.setCheckConfigJsonSignature(false);
        return exportedFile;
    }

    @Override
    public ExportedFile archiveFullData(Object data, ExportProperties metaData) throws Exception {
        return archiveFullData(data, metaData, null);
    }

    @Override
    public ExportRowDataAppender buildRowDataAppender(ExportProperties metaData)
            throws IOException {
        return buildRowDataAppender(metaData, null);
    }

    @Override
    public ExportRowDataAppender buildRowDataAppender(ExportProperties metaData,
            String encryptKey)
            throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(metaData.acquireConfigJsonFileUrl());
        createFileWithDirectories(file);
        JsonGenerator generator = jsonFactory.createGenerator(file, JsonEncoding.UTF8);
        generator.writeStartObject();
        generator.writeFieldName("metadata");
        objectMapper.writeValue(generator, metaData);
        generator.writeArrayFieldStart("data");
        return new JsonArchiverRowDataAppender(objectMapper, generator, metaData, encryptKey);
    }

    private File writeToFile(String fileName, String encrypted) throws IOException {
        try (FileWriter fileWriter = new FileWriter(fileName)) {
            fileWriter.write(encrypted);
            log.info("Archived file: {}", fileName);
        } catch (IOException e) {
            log.error("Failed to write json file", e);
            throw e;
        }
        return new File(fileName);
    }

    public static class JsonArchiverRowDataAppender implements ExportRowDataAppender {

        private final JsonGenerator jsonGenerator;
        private final ObjectMapper objectMapper;
        private final ExportProperties metaData;
        @Nullable
        private final String encryptKey;
        private final Map<String, InputStream> additionFiles = new HashMap<>();

        @Nullable
        private Mac mac;

        public JsonArchiverRowDataAppender(ObjectMapper objectMapper, JsonGenerator jsonGenerator,
                ExportProperties metaData, @Nullable String encryptKey) {
            this.objectMapper = objectMapper;
            this.jsonGenerator = jsonGenerator;
            this.metaData = metaData;
            this.encryptKey = encryptKey;
            if (encryptKey != null) {
                try {
                    Mac mac = Mac.getInstance(HMAC_ALGORITHM);
                    SecretKeySpec secretKeySpec = new SecretKeySpec(encryptKey.getBytes(), HMAC_ALGORITHM);
                    mac.init(secretKeySpec);
                    this.mac = mac;
                    mac.update(
                            ("metadata" + objectMapper.writeValueAsString(metaData)).getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public ExportProperties getMetaData() {
            return metaData;
        }

        @Override
        public void append(Encryptable encryptable) throws IOException {
            if (encryptKey != null) {
                encryptable.encrypt(encryptKey);
            }
            objectMapper.writeValue(jsonGenerator, encryptable);
            if (mac != null) {
                mac.update(objectMapper.writeValueAsString(encryptable).getBytes(StandardCharsets.UTF_8));
            }
        }

        @Override
        public void addAdditionFile(String fileName, InputStream inputStream) throws IOException {
            this.additionFiles.put(fileName, inputStream);
        }

        @Override
        public ExportedFile build() throws IOException {
            jsonGenerator.writeEndArray();
            if (mac != null) {
                jsonGenerator.writeFieldName("signature");
                String computedSignature = Base64.getEncoder().encodeToString(mac.doFinal());
                objectMapper.writeValue(jsonGenerator, computedSignature);
            }
            jsonGenerator.writeEndObject();
            jsonGenerator.close();
            String fileUrl = metaData.acquireConfigJsonFileUrl();
            return new ExportedZipFileFactory(new File(fileUrl), this.additionFiles).build(metaData.acquireZipFileUrl(),
                    encryptKey);
        }

        @Override
        public void close() throws IOException {
            jsonGenerator.close();
        }

    }

}
