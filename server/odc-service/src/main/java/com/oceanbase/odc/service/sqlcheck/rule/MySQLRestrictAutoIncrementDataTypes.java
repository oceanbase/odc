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
import com.oceanbase.tools.sqlparser.statement.common.DataType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnAttributes;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * {@link MySQLRestrictAutoIncrementDataTypes}
 *
 * @author yh263208
 * @date 2023-09-11 14:28
 * @since ODC_release_4.2.2
 */
public class MySQLRestrictAutoIncrementDataTypes implements SqlCheckRule {

    private final Set<String> allowedTypeNames;

    public MySQLRestrictAutoIncrementDataTypes(@NonNull Set<String> allowedTypeNames) {
        this.allowedTypeNames = allowedTypeNames;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_AUTO_INCREMENT_DATATYPES;
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
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private List<CheckViolation> builds(String sql, Stream<ColumnDefinition> stream) {
        return stream.filter(d -> {
            ColumnAttributes ca = d.getColumnAttributes();
            if (ca == null) {
                return false;
            }
            return Boolean.TRUE.equals(ca.getAutoIncrement());
        }).filter(d -> this.allowedTypeNames.stream()
                .noneMatch(s -> StringUtils.equalsIgnoreCase(s, d.getDataType().getName())))
                .map(d -> {
                    DataType type = d.getDataType();
                    String types = "N/A";
                    if (CollectionUtils.isNotEmpty(this.allowedTypeNames)) {
                        types = String.join(",", this.allowedTypeNames);
                    }
                    return SqlCheckUtil.buildViolation(sql, type, getType(),
                            new Object[] {d.getColumnReference().getText(), type.getText(), types});
                }).collect(Collectors.toList());
    }

}
