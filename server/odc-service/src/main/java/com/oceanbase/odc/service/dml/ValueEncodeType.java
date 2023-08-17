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
package com.oceanbase.odc.service.dml;

import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.oceanbase.odc.core.shared.PreConditions;

/**
 * The display form of the binary file can be in txt format or hexadecimal format
 *
 * @author yh263208
 * @date 2021-11-19 18:21
 * @since ODC_release_3.2.1
 */
public enum ValueEncodeType {
    /**
     * Encode with uft-8
     */
    TXT {
        @Override
        public String encodeToString(byte[] contents) {
            PreConditions.notNull(contents, "Contents");
            return new String(contents, StandardCharsets.UTF_8);
        }

        @Override
        public byte[] decode(String value) {
            if (value == null) {
                return null;
            }
            return value.getBytes(StandardCharsets.UTF_8);
        }
    },
    /**
     * Encode with hex
     */
    HEX {
        @Override
        public String encodeToString(byte[] contents) {
            PreConditions.notNull(contents, "Contents");
            String stringContents = Hex.encodeHexString(contents);
            StringBuilder stringBuilder = new StringBuilder();
            int length = stringContents.length();
            int skip = 2;
            for (int i = 0; i < length; i = i + skip) {
                int endIndex = i + skip;
                if (endIndex > length) {
                    endIndex = length;
                }
                stringBuilder.append(stringContents, i, endIndex).append(" ");
            }
            return stringBuilder.toString().toUpperCase();
        }

        @Override
        public byte[] decode(String value) {
            if (value == null) {
                return null;
            }
            try {
                return Hex.decodeHex(value.replaceAll(" ", ""));
            } catch (DecoderException e) {
                throw new IllegalStateException(e);
            }
        }
    };

    public abstract String encodeToString(byte[] contents);

    public abstract byte[] decode(String value);

}
