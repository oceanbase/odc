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
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.InLineConstraint;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;

import lombok.NonNull;

/**
 * {@link NoPrimaryKeyExists}
 *
 * @author yh263208
 * @date 2023-06-12 14:58
 * @since ODC_release_4.2.0
 */
public class NoPrimaryKeyExists implements SqlCheckRule {

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.NO_PRIMARY_KEY_EXISTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (!(statement instanceof CreateTable)) {
            return Collections.emptyList();
        }
        CreateTable createTable = (CreateTable) statement;
        if (Objects.nonNull(createTable.getLikeTable()) || Objects.nonNull(createTable.getAs())) {
            return Collections.emptyList();
        }
        boolean containsPk = createTable.getColumnDefinitions().stream()
                .filter(c -> c.getColumnAttributes() != null
                        && CollectionUtils.isNotEmpty(c.getColumnAttributes().getConstraints()))
                .flatMap(c -> c.getColumnAttributes().getConstraints().stream())
                .anyMatch(InLineConstraint::isPrimaryKey);
        containsPk |= createTable.getConstraints().stream().anyMatch(OutOfLineConstraint::isPrimaryKey);
        if (containsPk) {
            return Collections.emptyList();
        }
        return Collections.singletonList(SqlCheckUtil
                .buildViolation(statement.getText(), statement, getType(), new Object[] {}));
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL, DialectType.ORACLE);
    }

}
