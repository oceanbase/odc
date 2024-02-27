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

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createindex.CreateIndex;
import com.oceanbase.tools.sqlparser.statement.createtable.OutOfLineConstraint;

import lombok.NonNull;

/**
 * {@link IndexChangeTimeConsumingExists}
 *
 * @author jingtian
 * @date 2024/2/19
 * @since ODC_release_4.2.4
 */
public class IndexChangeTimeConsumingExists implements SqlCheckRule {

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.INDEX_CHANGE_TIME_CONSUMING_EXISTS;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.OB_ORACLE,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof AlterTable) {
            return ((AlterTable) statement).getAlterTableActions().stream().filter(action -> {
                if (action.getAddIndex() != null || action.getModifyPrimaryKey() != null) {
                    return true;
                } else if (action.getAddConstraint() != null) {
                    OutOfLineConstraint addConstraint = action.getAddConstraint();
                    return addConstraint.isPrimaryKey() || addConstraint.isUniqueKey();
                } else {
                    return false;
                }
            }).collect(Collectors.toList()).stream().map(action -> SqlCheckUtil.buildViolation(statement.getText(),
                    statement, getType(), new Object[] {})).collect(Collectors.toList());
        } else if (statement instanceof CreateIndex) {
            return Collections.singletonList(
                    SqlCheckUtil.buildViolation(statement.getText(), statement, getType(), new Object[] {}));
        }
        return Collections.emptyList();
    }
}
