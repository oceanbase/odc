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
package com.oceanbase.odc.service.ai.chat.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn.KeyType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/5/12
 */
public class MSchemaBuilder {

    public static String build(String table, String tableComment, List<DBTableColumn> fields,
            List<DBTableConstraint> foreignKeys) {
        List<String> output = new ArrayList<>();
        List<String> fieldLines = new ArrayList<>();

        StringBuilder header = new StringBuilder("# Table : " + table);
        if (StringUtils.isNotBlank(tableComment)) {
            header.append(", ").append(tableComment);
        }
        output.add(header.toString());

        if (fields != null) {
            for (DBTableColumn fieldInfo : fields) {
                String fieldName = fieldInfo.getName();

                String rawType = getFieldType(fieldInfo.getTypeName());
                String fieldLine = "(" + fieldName + ":" + rawType.toUpperCase();

                if (fieldInfo.getComment() != null && !fieldInfo.getComment().trim().isEmpty()) {
                    fieldLine += ", " + fieldInfo.getComment().trim();
                }

                // 打上主键标识
                if (fieldInfo.getKeyType() == KeyType.PRI) {
                    fieldLine += ", Primary Key";
                }

                // TODO：当前由于隐私问题，不支持示例
                Map<String, List<Object>> columnExamples = new HashMap<>();
                // 如果有示例，添加上
                List<Object> examples = columnExamples.get(fieldName);
                if (examples != null && !examples.isEmpty()) {
                    examples = examples.stream().filter(Objects::nonNull).collect(Collectors.toList());
                    String examplesStr = examplesToString(examples);

                    if (Arrays.asList("DATE", "TIME", "DATETIME", "TIMESTAMP").contains(rawType)) {
                        examplesStr = examplesStr.split(", ")[0];
                    } else if (examplesStr.split(", ").length > 0
                            && Arrays.stream(examplesStr.split(", ")).mapToInt(String::length).max().orElse(0) > 20) {
                        if (Arrays.stream(examplesStr.split(", ")).mapToInt(String::length).max().orElse(0) > 50) {
                            examplesStr = "";
                        } else {
                            examplesStr = examplesStr.split(", ")[0];
                        }
                    }

                    if (!examplesStr.isEmpty()) {
                        fieldLine += ", Examples: [" + examplesStr + "]";
                    }
                }

                fieldLine += ")";
                fieldLines.add(fieldLine);
            }
        }

        output.add("【Columns】\n [");
        output.add(String.join(",\n", fieldLines));
        output.add("]");

        if (foreignKeys != null && foreignKeys.stream().anyMatch(c -> c.getType() == DBConstraintType.FOREIGN_KEY)) {
            output.add("【Foreign keys】");
            for (DBTableConstraint fk : foreignKeys) {
                if (fk.getType() != DBConstraintType.FOREIGN_KEY) {
                    continue;
                }
                String table1 = fk.getTableName();
                String column1 = fk.getColumnNames().get(0); // 假设只有一个列名
                String table2 = fk.getReferenceTableName();
                String column2 = fk.getReferenceColumnNames().get(0); // 假设只有一个列名
                output.add(table1 + "." + column1 + "=" + table2 + "." + column2);
            }
        }

        return String.join("\n", output);
    }

    private static String getFieldType(String type) {
        // 这里需要根据实际需求实现获取字段类型的方法
        return type; // 示例中直接返回type
    }

    private static String examplesToString(List<Object> examples) {
        return examples.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

}
