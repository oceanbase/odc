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

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.split.OffsetString;
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
    private BaseAffectedRowsExceedLimit affectedRowRule;

    private AffectedRowCalculator() {}

    public AffectedRowCalculator(String delimiter, @NonNull DialectType dialectType,
            BaseAffectedRowsExceedLimit affectedRowRule) {
        this.delimiter = delimiter;
        this.dialectType = dialectType;
        this.affectedRowRule = affectedRowRule;
    }

    public AffectedRowCalculator(@NonNull DialectType dialectType,
            BaseAffectedRowsExceedLimit affectedRowRule) {
        this(null, dialectType, affectedRowRule);
    }

    public long getAffectedRows(@NonNull String sqlScript) {
        return getAffectedRows(SqlCheckUtil.splitSql(sqlScript, dialectType, delimiter));
    }

    public long getAffectedRows(@NonNull Collection<OffsetString> sqls) {
        long affectedRows = 0L;
        if (affectedRowRule != null) {
            for (OffsetString sql : sqls) {
                try {
                    Statement statement = SqlCheckUtil.parseSingleSql(dialectType, sql.getStr());
                    long statementAffectedRows = affectedRowRule.getStatementAffectedRows(statement);
                    if (statementAffectedRows > 0) {
                        affectedRows += statementAffectedRows;
                    }
                } catch (Exception e) {
                    log.warn("Get affected rows failed", e);
                }
            }
        } else {
            return -1;
        }
        return affectedRows;
    }
}
