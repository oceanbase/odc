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
package com.oceanbase.odc.service.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/23
 */
public class Constants {

    public static final String VIEW_DEFINITION_TEMPLATE;
    public static final String DATABASE_ID_KEY = "DATABASE_ID";
    public static final String CREATE_VECTOR_DDL_SQL_TEMPLATE;
    public static final String FIX_SQL_SYSTEM;
    public static final String FIX_SQL_USER;
    public static final String SQL_CORRECTION_SYSTEM;
    public static final String GENERATE_SQL_WITH_DDL_PROMPT;
    public static final String EXTRACT_PROMPT;
    public static final String COMPLETE_SQL_PROMPT;
    public static final String MODIFY_SQL_WITH_DDL_PROMPT;
    public static final String SQL_OPTIMIZER_WITH_DDL_PROMPT;
    public static final String SQL_DEBUGGING_WITH_DDL_PROMPT;
    public static final String SQL_FORMATTING_PROMPT;

    public static final String MYSQL_GRAMMAR = "MySQL";
    public static final String PRE_SURROUND = "<<";
    public static final String POST_SURROUND = ">>";
    public static final String ANNOTATION = "--";
    public static final String CURSOR = "<|>";

    static {
        CREATE_VECTOR_DDL_SQL_TEMPLATE = readResource("llm/copilot/template/sql/copilot_vector_template.sql");
        VIEW_DEFINITION_TEMPLATE = readResource("llm/copilot/template/prompt/view_definition_template.txt");
        FIX_SQL_SYSTEM = readResource("llm/copilot/template/prompt/fix_sql_system.txt");
        GENERATE_SQL_WITH_DDL_PROMPT = readResource("llm/copilot/template/prompt/generate_sql_with_ddl.txt");
        FIX_SQL_USER = readResource("llm/copilot/template/prompt/fix_sql_user.txt");
        SQL_CORRECTION_SYSTEM = readResource("llm/copilot/template/prompt/sql_correction.txt");
        EXTRACT_PROMPT = readResource("llm/copilot/template/prompt/extract.txt");
        COMPLETE_SQL_PROMPT = readResource("llm/copilot/template/prompt/complete_sql.txt");
        MODIFY_SQL_WITH_DDL_PROMPT = readResource("llm/copilot/template/prompt/modify_sql_with_ddl.txt");
        SQL_OPTIMIZER_WITH_DDL_PROMPT = readResource("llm/copilot/template/prompt/sql_optimizer_with_ddl.txt");
        SQL_DEBUGGING_WITH_DDL_PROMPT = readResource("llm/copilot/template/prompt/sql_debugging_with_ddl.txt");
        SQL_FORMATTING_PROMPT = readResource("llm/copilot/template/prompt/sql_formatting.txt");
    }

    private static String readResource(String resourceName) {
        try {
            ClassLoader classLoader = Constants.class.getClassLoader();
            URL resourceUrl = classLoader.getResource(resourceName);
            InputStream inputStream = resourceUrl.openStream();
            StringBuilder stringBuilder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read resource: " + resourceName, e);
        }
    }

}
