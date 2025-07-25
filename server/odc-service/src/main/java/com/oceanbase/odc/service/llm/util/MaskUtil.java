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
package com.oceanbase.odc.service.llm.util;

public class MaskUtil {
    public static String maskApiKey(String key) {
        if (key == null) {
            return null;
        }
        if (key.length() <= 8) {
            return key;
        }
        StringBuilder masked = new StringBuilder();
        masked.append(key.substring(0, 5));
        for (int i = 5; i < key.length() - 3; i++) {
            masked.append('*');
        }
        masked.append(key.substring(key.length() - 3));
        return masked.toString();
    }
}
