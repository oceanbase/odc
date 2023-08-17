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
package com.oceanbase.odc.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * @author yizhou.xw
 * @version : EncodeUtils.java, v 0.1 2020-04-16 12:53
 */
public class EncodeUtils {

    public static byte[] base64DecodeFromString(String encodedString) {
        if (Objects.isNull(encodedString)) {
            return null;
        }
        byte[] bytes = encodedString.getBytes(StandardCharsets.UTF_8);
        return base64Decode(bytes);
    }

    public static byte[] base64Decode(byte[] encoded) {
        if (Objects.isNull(encoded)) {
            return null;
        }
        return Base64.getDecoder().decode(encoded);
    }

    public static String base64EncodeToString(byte[] src) {
        byte[] encoded = base64Encode(src);
        if (Objects.isNull(encoded)) {
            return null;
        }
        return new String(encoded, StandardCharsets.UTF_8);
    }

    public static byte[] base64Encode(byte[] src) {
        if (Objects.isNull(src)) {
            return null;
        }
        return Base64.getEncoder().encode(src);
    }
}
