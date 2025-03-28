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
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.sql.split.OffsetString;
import com.oceanbase.odc.core.sql.split.SqlCommentProcessor;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.BaseAffectedRowsExceedLimit;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.insert.Insert;
import com.oceanbase.tools.sqlparser.statement.update.Update;

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

    private DefaultSqlChecker sqlChecker;
    private String delimiter;
    private DialectType dialectType;
    private List<SqlCheckRule> rules;

    private AffectedRowCalculator() {}

    public AffectedRowCalculator(@NonNull DefaultSqlChecker sqlChecker, String delimiter,
            @NonNull DialectType dialectType, @NonNull List<SqlCheckRule> rules) {
        this.sqlChecker = sqlChecker;
        this.delimiter = delimiter;
        this.dialectType = dialectType;
        this.rules = rules;
    }

    public AffectedRowCalculator(@NonNull DefaultSqlChecker sqlChecker, @NonNull DialectType dialectType,
            @NonNull List<SqlCheckRule> rules) {
        this(sqlChecker, null, dialectType, rules);
    }

    public long getAffectedRows(@NonNull String sqlScript) {
        return getAffectedRows(splitSql(sqlScript));
    }

    public long getAffectedRows(@NonNull Collection<OffsetString> sqls) {
        long affectedRows = 0L;
        Optional<SqlCheckRule> calculatorOp = getCalculatorRule();
        if (calculatorOp.isPresent() && (calculatorOp.get().getRule() instanceof BaseAffectedRowsExceedLimit)) {
            BaseAffectedRowsExceedLimit calculator = (BaseAffectedRowsExceedLimit) (calculatorOp.get().getRule());
            for (OffsetString sql : sqls) {
                try {
                    Statement statement = sqlChecker.doParse(sql.getStr());
                    checkSupport(statement);
                    affectedRows += calculator.getStatementAffectedRows(statement);
                } catch (Exception e) {
                    log.warn("Get affected rows failed", e);
                }
            }
        }
        return affectedRows;
    }

    private List<OffsetString> splitSql(String sqlScript) {
        SqlCommentProcessor processor = new SqlCommentProcessor(dialectType, true, true);
        processor.setDelimiter(delimiter);
        StringBuffer buffer = new StringBuffer();
        List<OffsetString> sqls = processor.split(buffer, sqlScript);
        String bufferStr = buffer.toString();
        if (!bufferStr.trim().isEmpty()) {
            int lastSqlOffset;
            if (sqls.isEmpty()) {
                int index = sqlScript.indexOf(bufferStr.trim());
                lastSqlOffset = index == -1 ? 0 : index;
            } else {
                int from = sqls.get(sqls.size() - 1).getOffset() + sqls.get(sqls.size() - 1).getStr().length();
                int index = sqlScript.indexOf(bufferStr.trim(), from);
                lastSqlOffset = index == -1 ? from : index;
            }
            sqls.add(new OffsetString(lastSqlOffset, bufferStr));
        }
        return sqls;
    }

    private Optional<SqlCheckRule> getCalculatorRule() {
        return rules.stream()
                .filter(r -> r.getSupportsDialectTypes().contains(dialectType))
                .filter(r -> r.getType() == SqlCheckRuleType.RESTRICT_SQL_AFFECTED_ROWS)
                .findFirst();
    }

    private void checkSupport(@NonNull Statement statement) {
        if ((statement instanceof Insert) || (statement instanceof Update) || (statement instanceof Delete)) {
            return;
        }
        throw new UnsupportedException("Unsupported statement type: " + statement.getClass().getName());
    }

}
