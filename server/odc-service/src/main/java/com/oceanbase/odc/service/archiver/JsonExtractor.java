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
package com.oceanbase.odc.service.archiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.EncryptAlgorithm;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonExtractor implements Extractor {

    public <D> ArchivedData<D> extractFullData(ArchivedFile archivedFile, TypeReference<ArchivedData<D>> typeReference)
            throws Exception {
        try (InputStream inputStream = archivedFile.getProvider().getInputStream()) {
            String decryptedString = decrypt(inputStream, archivedFile.getSecret());
            return JsonUtils.fromJson(decryptedString, typeReference);
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
}
