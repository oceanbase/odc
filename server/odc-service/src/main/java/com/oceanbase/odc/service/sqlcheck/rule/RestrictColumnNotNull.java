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
package com.oceanbase.odc.service.sqlcheck.rule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;

import lombok.NonNull;

/**
 * {@link RestrictColumnNotNull}
 *
 * @author yh263208
 * @date 2023-06-26 18:12
 * @since ODC_release_4.2.0
 */
public class RestrictColumnNotNull implements SqlCheckRule {

    private final Set<String> nullableDatatypes;

    public RestrictColumnNotNull(@NonNull Set<String> nullableDatatypes) {
        this.nullableDatatypes = nullableDatatypes;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.COLUMN_IS_NULLABLE;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            return builds(statement.getText(), ((CreateTable) statement).getColumnDefinitions().stream());
        } else if (statement instanceof AlterTable) {
            return builds(statement.getText(), SqlCheckUtil.fromAlterTable((AlterTable) statement));
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_ORACLE, DialectType.MYSQL, DialectType.OB_MYSQL,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private boolean containsIgnoreCase(String name) {
        if (name == null) {
            return true;
        }
        return this.nullableDatatypes.stream().anyMatch(s -> StringUtils.equalsIgnoreCase(s, name));
    }

    private List<CheckViolation> builds(String sql, Stream<ColumnDefinition> stream) {
        return stream.filter(d -> {
            ColumnAttributes attributes = d.getColumnAttributes();
            if (attributes == null) {
                return !containsIgnoreCase(getDataTypeName(d));
            }
            List<InLineConstraint> cs = attributes.getConstraints();
            if (CollectionUtils.isEmpty(cs)) {
                return !containsIgnoreCase(getDataTypeName(d));
            }
            return cs.stream().noneMatch(c -> Boolean.FALSE.equals(c.getNullable()))
                    && !containsIgnoreCase(getDataTypeName(d));
        }).map(d -> {
            String dataTypes = "N/A";
            if (CollectionUtils.isNotEmpty(nullableDatatypes)) {
                dataTypes = String.join(",", nullableDatatypes);
            }
            return SqlCheckUtil.buildViolation(sql, d, getType(), new Object[] {d.getDataType().getText(), dataTypes});
        }).collect(Collectors.toList());
    }

    private String getDataTypeName(ColumnDefinition definition) {
        return definition.getDataType() == null ? null : definition.getDataType().getName();
    }

}
