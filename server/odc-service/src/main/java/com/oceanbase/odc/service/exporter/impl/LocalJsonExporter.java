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

import java.io.File;
import java.io.IOException;
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
import com.oceanbase.odc.service.common.util.OdcFileUtil;
import com.oceanbase.odc.service.exporter.Exporter;
import com.oceanbase.odc.service.exporter.model.Encryptable;
import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportRowDataAppender;
import com.oceanbase.odc.service.exporter.model.ExportedFile;
import com.oceanbase.odc.service.exporter.model.ExportedZipFileBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalJsonExporter implements Exporter {

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
        JsonGenerator generator = jsonFactory.createGenerator(file, JsonEncoding.UTF8);
        generator.writeStartObject();
        generator.writeFieldName("metadata");
        objectMapper.writeValue(generator, metaData);
        generator.writeArrayFieldStart("data");
        return new JsonArchiverRowDataAppender(objectMapper, generator, metaData, encryptKey);
    }

    public static class JsonArchiverRowDataAppender implements ExportRowDataAppender {

        private final JsonGenerator jsonGenerator;
        private final ObjectMapper objectMapper;
        private final ExportProperties metaData;
        @Nullable
        private final String encryptKey;
        private final Map<String, File> additionFiles = new HashMap<>();

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
        public void addAdditionFile(String fileName, File file) throws IOException {
            this.additionFiles.put(fileName, file);
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
            File configFile = new File(fileUrl);
            ExportedFile build = new ExportedZipFileBuilder(configFile, this.additionFiles).build(
                    metaData.acquireZipFileUrl(),
                    encryptKey);
            OdcFileUtil.deleteFiles(configFile);
            return build;
        }

        @Override
        public void close() throws IOException {
            jsonGenerator.close();
        }

    }

}
