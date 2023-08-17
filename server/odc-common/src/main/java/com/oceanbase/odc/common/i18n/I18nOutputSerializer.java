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
package com.oceanbase.odc.common.i18n;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class I18nOutputSerializer extends JsonSerializer<String> {
    private static final Pattern I18N_RESOURCE_PATTERN = Pattern.compile("\\$\\{([^{^}]*)\\}");
    private static final Map<Locale, Map<String, String>> MESSAGES_MAP = new HashMap<>();

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(convertString(value));
    }

    /**
     * 每次调用时，判断是否有内置资源需要翻译，判断map中是否有待翻译资源，最后调用StringSubstitutor完成替换
     *
     * @param resource
     * @return
     */
    protected String convertString(String resource) {
        Matcher matcher = I18N_RESOURCE_PATTERN.matcher(resource);
        if (!matcher.find()) {
            return resource;
        }

        Locale currentLocale = LocaleContextHolder.getLocale();
        MESSAGES_MAP.putIfAbsent(currentLocale, new HashMap<>());
        int cursor = 0;
        while (matcher.find(cursor)) {
            String key = matcher.group();
            key = key.substring(2, key.length() - 1);
            MESSAGES_MAP.get(currentLocale).putIfAbsent(key, I18n.translate(key, null, key, currentLocale));

            cursor = matcher.end();
        }
        StringSubstitutor sub =
                new StringSubstitutor(MESSAGES_MAP.get(currentLocale)).setDisableSubstitutionInValues(true);
        return sub.replace(resource);
    }
}
