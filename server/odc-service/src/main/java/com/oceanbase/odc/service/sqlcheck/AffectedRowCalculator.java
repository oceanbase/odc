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
package com.oceanbase.odc.service.sqlcheck;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.BaseAffectedRowsExceedLimit;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: ysj
 * @Date: 2025/3/27 20:25
 * @Since: 4.3.4
 * @Description: to calc sql affected Rows
 */
@Slf4j
public class AffectedRowCalculator {

    private String delimiter;
    private DialectType dialectType;
    private List<SqlCheckRule> rules;

    private AffectedRowCalculator() {}

    public AffectedRowCalculator(String delimiter, @NonNull DialectType dialectType,
            @NonNull List<SqlCheckRule> rules) {
        this.delimiter = delimiter;
        this.dialectType = dialectType;
        this.rules = rules;
    }

    public AffectedRowCalculator(@NonNull DialectType dialectType, @NonNull List<SqlCheckRule> rules) {
        this(null, dialectType, rules);
    }

    public long getAffectedRows(@NonNull String sqlScript) {
        return getAffectedRows(SqlCheckUtil.splitSql(sqlScript, dialectType, delimiter));
    }

    public long getAffectedRows(@NonNull Collection<OffsetString> sqls) {
        long affectedRows = 0L;
        Optional<SqlCheckRule> calculatorOp = getCalculatorRule();
        if (calculatorOp.isPresent() && (calculatorOp.get().getRule() instanceof BaseAffectedRowsExceedLimit)) {
            BaseAffectedRowsExceedLimit calculator = (BaseAffectedRowsExceedLimit) (calculatorOp.get().getRule());
            for (OffsetString sql : sqls) {
                try {
                    Statement statement = SqlCheckUtil.parseSingleSql(dialectType, sql.getStr());
                    affectedRows += calculator.getStatementAffectedRows(statement);
                } catch (Exception e) {
                    log.warn("Get affected rows failed", e);
                }
            }
        }
        return affectedRows;
    }

    private Optional<SqlCheckRule> getCalculatorRule() {
        return rules.stream()
                .filter(r -> r.getSupportsDialectTypes().contains(dialectType))
                .filter(r -> r.getType() == SqlCheckRuleType.RESTRICT_SQL_AFFECTED_ROWS)
                .findFirst();
    }

}
