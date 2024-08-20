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

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;

import lombok.NonNull;

/**
 * {@link MySQLAffectedRows}
 *
 * @author yiminpeng
 * @version 1.0
 * @date 2024-08-01 18:18
 */
public class MySQLAffectedRows implements SqlCheckRule {

    private final Integer maxSQLAffectedRows;

    private final JdbcOperations jdbc;

    public MySQLAffectedRows(@NonNull Integer maxSQLAffectedRows, JdbcOperations jdbcOperations) {
        this.maxSQLAffectedRows = maxSQLAffectedRows <= 0 ? 0 : maxSQLAffectedRows;
        this.jdbc = jdbcOperations;
    }

    /**
     * 获取规则类型
     */
    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_SQL_AFFECTED_ROWS;
    }

    /**
     * 执行规则检查
     */
    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {

        return Collections.emptyList();
    }

    /**
     * 获取支持的数据库类型
     */
    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL);
    }

    private long executeExplain(String sql, JdbcOperations jdbc) {
        String explainQuery = "EXPLAIN " + sql;
        try (ResultSet resultSet = jdbc.query(explainQuery, rs -> rs)) {
            /*
             * EXPLAIN 执行结果会以表的形式返回
             * 在解析所得的结果集里 可能存在多张表
             * 其中，第一个不为空的 rows 值即为预估影响行数
             */
            if (resultSet != null) {
                while (resultSet.next()) {
                    long rows = resultSet.getLong("rows");
                    if (rows != 0) {
                        return rows;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute sql: " + explainQuery, e);
        }
        return 0;
    }
}
