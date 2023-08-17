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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.springframework.context.support.ReloadableResourceBundleMessageSource;

public class ExposedResourceMessageBundleSource extends ReloadableResourceBundleMessageSource {

    /**
     * 通过封装ReloadableResourceBundleMessageSource类，调用其protected方法，获取指定语言的map
     *
     * @param locale
     * @return
     */
    public Map<String, String> getAllMessages(Locale locale) {
        Properties properties = getMergedProperties(locale).getProperties();
        Map<String, String> messagesMap = new HashMap<>();
        if (!Objects.isNull(properties)) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                messagesMap.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return messagesMap;
    }

}
