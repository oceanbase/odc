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
package com.oceanbase.odc.common.security;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;

public class SensitiveDataUtils {

    private static final Pattern SENSITIVE_PATTERN =
            Pattern.compile("(secret|key|password|pswd|email|-p)([=|:|\\\"\\s]*)([^&,\\n\\t\\\"]+)",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern SENSITIVE_KEY_PATTERN =
            Pattern.compile("(secret|key|password|pswd|email)", Pattern.CASE_INSENSITIVE);
    private static final String MASKED_VALUE = "***";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int DEFAULT_MAX_DEPTH = 100;

    public static String maskJson(String json, int maxDepth) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            maskSensitiveFields(rootNode, 0, maxDepth);
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            String message = "MESSAGE_MASK_FAILED, origin message start with " + json.substring(0, 10);
            Map<String, String> map = new HashMap<>();
            map.put("msg", message);
            return JsonUtils.toJson(map);
        }
    }

    public static String maskJson(String json) {
        return maskJson(json, DEFAULT_MAX_DEPTH);
    }

    private static void maskSensitiveFields(JsonNode node, int currentDepth, int maxDepth) {
        if (currentDepth > maxDepth) {
            throw new RuntimeException("Json size exceeds max depth: " + maxDepth);
        }
        if (node.isObject()) {
            ObjectNode objNode = (ObjectNode) node;
            Iterator<String> fieldNames = objNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode childNode = objNode.get(fieldName);

                if (SENSITIVE_KEY_PATTERN.matcher(fieldName).find()) {
                    objNode.set(fieldName, TextNode.valueOf(MASKED_VALUE));
                } else if (childNode.isTextual() && SENSITIVE_PATTERN.matcher(childNode.asText()).find()) {
                    objNode.set(fieldName, TextNode.valueOf(mask(childNode.asText())));
                } else {
                    maskSensitiveFields(childNode, currentDepth + 1, maxDepth);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode arrayItem : node) {
                maskSensitiveFields(arrayItem, currentDepth + 1, maxDepth);
            }
        }
    }

    public static String mask(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        try {
            Matcher matcher = SENSITIVE_PATTERN.matcher(message);
            if (matcher.find()) {
                StringBuffer sb = new StringBuffer();
                do {
                    matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2) + MASKED_VALUE);
                } while (matcher.find());
                matcher.appendTail(sb);
                return sb.toString();
            }
            return message;
        } catch (Exception ex) {
            return "MESSAGE_MASK_FAILED, origin message start with " + StringUtils.substring(message, 0, 10);
        }
    }
}
