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
package com.oceanbase.odc.service.integration.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;

/**
 * @author gaoda.xy
 * @date 2023/3/28 19:52
 */
public class TemplateVariables implements Serializable {
    private final Map<String, Serializable> variables;

    public TemplateVariables() {
        this.variables = new HashMap<>();
    }

    public TemplateVariables(Map<String, Serializable> variables) {
        this.variables = new HashMap<>(variables);
    }

    public void setAttribute(Variable variable, Serializable value) {
        this.variables.put(variable.key, value);
    }

    public void setAttribute(Variable variable, String subKey, Serializable value) {
        this.variables.put(variable.key + "." + subKey, value);
    }

    public String process(String source) {
        StringSubstitutor sub = new StringSubstitutor(this.variables).setDisableSubstitutionInValues(true);
        return sub.replace(source);
    }

    public enum Variable {
        ODC_SITE_URL("odc.site.url"),
        PROCESS_INSTANCE_ID("process.instance.id"),
        TASK_TYPE("task.type"),
        TASK_DETAILS("task.details"),
        USER_ID("user.id"),
        USER_NAME("user.name"),
        USER_ACCOUNT("user.account"),
        CONNECTION_NAME("connection.name"),
        CONNECTION_TENANT("connection.tenant"),
        CONNECTION_PROPERTIES("connection.properties"),
        SQL_CONTENT("sql.content"),
        SQL_CONTENT_JSON_ARRAY("sql.content.json.array");

        private final String key;

        public String key() {
            return key;
        }

        Variable(String key) {
            this.key = key;
        }
    }
}
