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

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;

import lombok.NonNull;

/**
 * {@link TooManyColumnRefInPrimaryKey}
 *
 * @author yh263208
 * @date 2023-06-19 14:52
 * @since ODC_release_4.2.0
 */
public class TooManyColumnRefInPrimaryKey implements SqlCheckRule {

    private final Integer maxColumnRefsCount;

    public TooManyColumnRefInPrimaryKey(@NonNull Integer maxColumnRefsCount) {
        this.maxColumnRefsCount = maxColumnRefsCount <= 0 ? 1 : maxColumnRefsCount;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TOO_MANY_COL_REFS_IN_PRIMARY_KEY;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            CreateTable createTable = (CreateTable) statement;
            if (CollectionUtils.isEmpty(createTable.getTableElements())) {
                return Collections.emptyList();
            }
            return createTable.getTableElements().stream().filter(e -> {
                if (e instanceof OutOfLineConstraint) {
                    OutOfLineConstraint c = (OutOfLineConstraint) e;
                    return c.isPrimaryKey() && c.getColumns().size() > maxColumnRefsCount;
                }
                return false;
            }).map(e -> {
                int size = ((OutOfLineConstraint) e).getColumns().size();
                return SqlCheckUtil.buildViolation(statement.getText(), e,
                        getType(), new Object[] {size, maxColumnRefsCount});
            }).collect(Collectors.toList());
        } else if (statement instanceof AlterTable) {
            AlterTable alterTable = (AlterTable) statement;
            return alterTable.getAlterTableActions().stream().filter(a -> {
                if (a.getAddConstraint() != null) {
                    OutOfLineConstraint c = a.getAddConstraint();
                    return c.isPrimaryKey() && c.getColumns().size() > maxColumnRefsCount;
                }
                return false;
            }).map(a -> {
                int size = a.getAddConstraint().getColumns().size();
                return SqlCheckUtil.buildViolation(statement.getText(), a,
                        getType(), new Object[] {size, maxColumnRefsCount});
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
