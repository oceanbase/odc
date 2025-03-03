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

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.truncate.TruncateTable;

import lombok.NonNull;

/**
 * {@link TruncateTableExists}
 *
 * @author yh263208
 * @date 2024-03-05 22:43
 * @since ODC_release_4.2.4
 */
public class TruncateTableExists implements SqlCheckRule {

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TRUNCATE_TABLE_EXISTS;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        return (statement instanceof TruncateTable)
                ? Collections.singletonList(SqlCheckUtil.buildViolation(
                        statement.getText(), statement, getType(), new Object[] {}))
                : Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.ORACLE, DialectType.MYSQL,
                DialectType.OB_ORACLE, DialectType.OB_MYSQL, DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
