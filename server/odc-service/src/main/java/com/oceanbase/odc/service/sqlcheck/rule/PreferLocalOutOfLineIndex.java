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
import com.oceanbase.tools.sqlparser.statement.createtable.ConstraintState;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.IndexOptions;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.TableElement;

import lombok.NonNull;

/**
 * {@link PreferLocalOutOfLineIndex}
 *
 * @author yh263208
 * @date 2022-12-26 20:13
 * @since ODC_release_4.1.0
 * @see SqlCheckRule
 */
public class PreferLocalOutOfLineIndex implements SqlCheckRule {

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof CreateTable)) {
            return Collections.emptyList();
        }
        CreateTable createTable = (CreateTable) statement;
        if (CollectionUtils.isEmpty(createTable.getTableElements())) {
            return Collections.emptyList();
        }
        List<TableElement> indexes = createTable.getTableElements().stream().filter(
                e -> e instanceof OutOfLineIndex).filter(e -> {
                    IndexOptions options = ((OutOfLineIndex) e).getIndexOptions();
                    return options == null || options.getGlobal() == null || options.getGlobal();
                }).collect(Collectors.toList());
        indexes.addAll(createTable.getTableElements().stream().filter(
                e -> e instanceof OutOfLineConstraint).filter(e -> {
                    ConstraintState state = ((OutOfLineConstraint) e).getState();
                    if (state == null || !state.isUsingIndexFlag()) {
                        return false;
                    }
                    IndexOptions options = state.getIndexOptions();
                    return options == null || options.getGlobal() == null || options.getGlobal();
                }).collect(Collectors.toList()));
        return indexes.stream().map(t -> SqlCheckUtil.buildViolation(
                statement.getText(), t, getType(), new Object[] {})).collect(Collectors.toList());
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.PREFER_LOCAL_INDEX;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_MYSQL, DialectType.MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
