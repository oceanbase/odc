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

import java.util.Collections;
import java.util.List;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Unable2JudgeAffectedRows implements SqlCheckRule {

    private final BaseAffectedRowsExceedLimit targetRule;

    public Unable2JudgeAffectedRows(@NonNull BaseAffectedRowsExceedLimit targetRule) {
        this.targetRule = targetRule;
    }

    /**
     * Get the rule type
     */
    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.ESTIMATE_SQL_AFFECTED_ROWS_FAILED;
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return this.targetRule.getSupportsDialectTypes();
    }

    /**
     * Execution rule check
     */
    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        try {
            long affectedRows = this.targetRule.getStatementAffectedRows(statement);
            if (affectedRows < 0) {
                return Collections.singletonList(SqlCheckUtil
                        .buildViolation(statement.getText(), statement, getType(), new Object[] {}));
            }
        } catch (Exception e) {
            log.warn("Unable to get affected rows, sql={}", statement.getText(), e);
            return Collections.singletonList(SqlCheckUtil
                    .buildViolation(statement.getText(), statement, getType(), new Object[] {}));
        }
        return Collections.emptyList();
    }
}
