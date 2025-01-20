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
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;

import lombok.NonNull;

/**
 * {@link TooManyColumnDefinition}
 *
 * @author yh263208
 * @date 2022-12-26 18:20
 * @since ODC_release_4.1.0
 * @see SqlCheckRule
 */
public class TooManyColumnDefinition implements SqlCheckRule {

    private final Integer maxColumnDefinitionCount;

    public TooManyColumnDefinition(@NonNull Integer maxColumnDefinitionCount) {
        this.maxColumnDefinitionCount = maxColumnDefinitionCount <= 0 ? 1 : maxColumnDefinitionCount;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof CreateTable)) {
            return Collections.emptyList();
        }
        CreateTable createTable = (CreateTable) statement;
        if (CollectionUtils.isEmpty(createTable.getTableElements())) {
            return Collections.emptyList();
        }
        List<TableElement> elts = createTable.getTableElements().stream().filter(t -> t instanceof ColumnDefinition)
                .collect(Collectors.toList());
        if (elts.size() < maxColumnDefinitionCount) {
            return Collections.emptyList();
        }
        TableElement lastOne = elts.get(elts.size() - 1);
        String tableName = StringUtils.unwrap(createTable.getTableName(), "\"");
        tableName = StringUtils.unwrap(tableName, "`");
        return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(), lastOne, getType(),
                new Object[] {tableName, maxColumnDefinitionCount, elts.size()}));
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TOO_MANY_COLUMNS;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
