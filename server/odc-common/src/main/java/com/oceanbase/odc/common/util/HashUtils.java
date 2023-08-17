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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;

/**
 * Hash工具类
 *
 * @author yizhou.xw
 * @date 2021-04-06
 **/
public class HashUtils {

    public static String sha1(byte[] inputs) {
        return Hashing.sha1().hashBytes(inputs).toString();
    }

    public static String sha1(String originalString) {
        return Hashing.sha1().hashString(originalString, StandardCharsets.UTF_8).toString();
    }

    public static String sha1(File file) throws IOException {
        return Files.hash(file, Hashing.sha1()).toString();
    }

    public static String sha256(String originalString) {
        return Hashing.sha256().hashString(originalString, StandardCharsets.UTF_8).toString();
    }

    public static String md5(byte[] inputs) {
        return Hashing.md5().hashBytes(inputs).toString();
    }

    public static String md5(String originalString) {
        return Hashing.md5().hashString(originalString, StandardCharsets.UTF_8).toString();
    }

}
