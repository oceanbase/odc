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

import org.apache.commons.text.StringSubstitutor;
import org.springframework.util.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;

/**
 * @Author: Lebie
 * @Date: 2023/3/21 17:13
 * @Description: []
 */
public class MessageTemplateProcessor {
    private static final String VARIABLE_PREFIX = "${";
    private static final String VARIABLE_SUFFIX = "}";

    public static String replaceVariables(final String template, final Map<String, String> variables) {
        if (StringUtils.isEmpty(template)) {
            return "";
        }
        if (CollectionUtils.isEmpty(variables)) {
            return template;
        }
        StringSubstitutor sub =
                new StringSubstitutor(variables, VARIABLE_PREFIX, VARIABLE_SUFFIX).setDisableSubstitutionInValues(true);
        return sub.replace(template);
    }
}
