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
package com.oceanbase.odc.service.notification.helper;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.json.JsonUtils;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/22
 */
public class MessageResponseValidator {


    public static Boolean validateMessage(String message, String responseValidation) {
        String trimMessage = StringUtils.trim(message);
        if (responseValidation.startsWith("{") && responseValidation.endsWith("}")) {
            return validateJsonMessage(trimMessage, responseValidation);
        }
        Pattern pattern = Pattern.compile(responseValidation, Pattern.DOTALL);
        return pattern.matcher(trimMessage).matches();
    }

    private static Boolean validateJsonMessage(String message, String responseValidation) {
        Map<String, Object> messageKV = JsonUtils.fromJsonMap(message,
                String.class, Object.class);
        Map<String, Object> validateKV = JsonUtils.fromJsonMap(responseValidation,
                String.class, Object.class);
        if (messageKV == null) {
            return false;
        }
        if (CollectionUtils.isEmpty(validateKV) && CollectionUtils.isEmpty(messageKV)) {
            return true;
        }
        return validateMap(messageKV, validateKV);
    }

    private static Boolean validateMap(Map<String, Object> message, Map<String, Object> validation) {
        for (String key : validation.keySet()) {
            if (!message.containsKey(key)) {
                return false;
            }
            boolean validateIsMap = validation.get(key) instanceof Map;
            boolean messageIsMap = message.get(key) instanceof Map;
            if (validateIsMap) {
                if (!messageIsMap || !validateMap((Map<String, Object>) message.get(key),
                        (Map<String, Object>) validation.get(key))) {
                    return false;
                }
            } else if (!message.get(key).equals(validation.get(key))) {
                return false;
            }
        }
        return true;
    }

}
