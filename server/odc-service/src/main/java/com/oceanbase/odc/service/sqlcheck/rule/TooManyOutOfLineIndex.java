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

import org.apache.commons.lang3.StringUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;

import lombok.NonNull;

/**
 * {@link TooManyOutOfLineIndex}
 *
 * @author yh263208
 * @date 2022-12-26 19:54
 * @since ODC_release_4.1.0
 */
public class TooManyOutOfLineIndex implements SqlCheckRule {

    private final Integer maxIndexCount;

    public TooManyOutOfLineIndex(@NonNull Integer maxIndexCount) {
        this.maxIndexCount = maxIndexCount < 0 ? 0 : maxIndexCount;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof CreateTable)) {
            return Collections.emptyList();
        }
        CreateTable createTable = (CreateTable) statement;
        List<TableElement> indexes = createTable.getTableElements().stream().filter(t -> {
            if (t instanceof OutOfLineIndex) {
                return true;
            } else if (t instanceof OutOfLineConstraint) {
                return ((OutOfLineConstraint) t).isUniqueKey();
            }
            return false;
        }).collect(Collectors.toList());
        if (indexes.size() < maxIndexCount) {
            return Collections.emptyList();
        }
        TableElement lastOne = indexes.get(indexes.size() - 1);
        String tableName = StringUtils.unwrap(createTable.getTableName(), "\"");
        tableName = StringUtils.unwrap(tableName, "`");
        return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                lastOne, getType(), new Object[] {tableName, maxIndexCount}));
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TOO_MANY_INDEX_KEYS;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
