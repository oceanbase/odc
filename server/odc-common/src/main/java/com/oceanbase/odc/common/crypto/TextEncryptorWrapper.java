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
package com.oceanbase.odc.common.crypto;

import java.nio.charset.StandardCharsets;

import com.oceanbase.odc.common.encode.ByteArrayToStringConverter;

/**
 * @author yizhou.xw
 * @version : TextEncryptorWrapper.java, v 0.1 2020-04-27 15:12
 */
public class TextEncryptorWrapper implements TextEncryptor {

    private final BytesEncryptor encryptDecrypt;
    private final ByteArrayToStringConverter byteArrayToStringConverter;

    public TextEncryptorWrapper(BytesEncryptor encryptDecrypt, ByteArrayToStringConverter byteArrayToStringConverter) {
        this.encryptDecrypt = encryptDecrypt;
        this.byteArrayToStringConverter = byteArrayToStringConverter;
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        byte[] bytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptDecrypt.encrypt(bytes);
        return byteArrayToStringConverter.convertToRight(encrypted);
    }

    @Override
    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            return null;
        }
        byte[] encrypted = byteArrayToStringConverter.convertToLeft(encryptedText);
        byte[] decrypt = encryptDecrypt.decrypt(encrypted);
        return new String(decrypt, StandardCharsets.UTF_8);
    }
}
