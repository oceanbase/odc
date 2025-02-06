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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonExtractor implements Extractor {

    public <D> ArchivedData<D> extractFullData(ArchivedFile archivedFile, TypeReference<ArchivedData<D>> typeReference)
            throws Exception {
        try (InputStream inputStream = archivedFile.getProvider().getInputStream()) {
            String decryptedString = decryptAESStream(inputStream, archivedFile.getSecret());
            return JsonUtils.fromJson(decryptedString, typeReference);
        }
    }

    private String decryptAESStream(InputStream inputStream, @Nullable String key) throws Exception {
        if (key == null) {
            return convertInputStreamToString(inputStream);
        }
        SecretKey secretKey = getKey(key.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        try (CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String convertInputStreamToString(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append(System.lineSeparator());
            }
        }

        return stringBuilder.toString();
    }

    private SecretKey getKey(byte[] keyBytes) throws Exception {
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.setSeed(keyBytes);
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256, random);
        return generator.generateKey();
    }


}
