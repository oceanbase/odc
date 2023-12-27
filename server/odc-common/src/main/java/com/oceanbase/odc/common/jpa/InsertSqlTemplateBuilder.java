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

package com.oceanbase.odc.common.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.persistence.metamodel.SingularAttribute;

import com.google.common.base.Joiner;
import com.oceanbase.odc.common.util.StringUtils;

public final class InsertSqlTemplateBuilder {

    private String tableName;
    private final List<String> fields = new ArrayList<>();

    public static InsertSqlTemplateBuilder from(String tableName) {
        InsertSqlTemplateBuilder insertSqlTemplateBuilder = new InsertSqlTemplateBuilder();
        insertSqlTemplateBuilder.tableName = tableName;
        return insertSqlTemplateBuilder;
    }

    public InsertSqlTemplateBuilder field(SingularAttribute<?, ?> attr) {
        fields.add(StringUtils.camelCaseToSnakeCase(attr.getName()));
        return this;
    }

    public InsertSqlTemplateBuilder field(String attr) {
        fields.add(attr);
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO `");
        sb.append(tableName);
        sb.append("` (");
        sb.append(String.join(",", fields));
        sb.append(") VALUES (");
        String value = Joiner.on(",").join(IntStream.range(0, fields.size()).mapToObj(i -> "?")
                .collect(Collectors.toList()));
        sb.append(value);
        sb.append(");");
        return sb.toString();
    }

}
