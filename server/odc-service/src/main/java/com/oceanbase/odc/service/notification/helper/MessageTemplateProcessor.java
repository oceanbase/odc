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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2023/3/21 17:13
 * @Description: []
 */
public class MessageTemplateProcessor {
    private static final Map<Locale, StringSubstitutor> LOCALE2SUBSTITUTOR = new HashMap<>();

    public static String replaceVariables(final String template, Locale locale, final Map<String, String> variables) {
        if (StringUtils.isEmpty(template)) {
            return "";
        }
        if (CollectionUtils.isEmpty(variables)) {
            return template;
        }
        if (variables.containsKey("taskType")) {
            String taskTypeI18nKey = String.format("${com.oceanbase.odc.TaskType.%s}", variables.get("taskType"));
            variables.put("taskType", taskTypeI18nKey);
        }
        if (variables.containsKey("taskStatus")) {
            String taskStatusI18nKey = String.format("${com.oceanbase.odc.event.%s.name}", variables.get("taskStatus"));
            variables.put("taskStatus", taskStatusI18nKey);
        }
        StringSubstitutor sub =
                new StringSubstitutor(variables).setDisableSubstitutionInValues(true);
        String message = sub.replace(template);

        if (Objects.nonNull(locale)) {
            StringSubstitutor i18n =
                    LOCALE2SUBSTITUTOR.computeIfAbsent(locale, l -> new StringSubstitutor(I18n.getAllMessages(l)));
            message = i18n.replace(message);
        }
        return message;
    }
}
