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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;

import lombok.NonNull;

/**
 * {@link MySQLRestrictTableAutoIncrement}
 *
 * @author yh263208
 * @date 2023-06-27 11:51
 * @since ODC_release_4.2.0
 */
public class MySQLRestrictTableAutoIncrement implements SqlCheckRule {

    private final Integer initValue;

    public MySQLRestrictTableAutoIncrement(@NonNull Integer initValue) {
        this.initValue = initValue;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_TABLE_AUTO_INCREMENT;
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
        TableOptions options = createTable.getTableOptions();
        if (options == null || options.getAutoIncrement() == null) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(
                    statement.getText(), statement, getType(), new Object[] {initValue, "N/A"}));
        } else if (options.getAutoIncrement().compareTo(new BigDecimal(initValue)) != 0) {
            return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(),
                    options, getType(), new Object[] {initValue, options.getAutoIncrement().toString()}));
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.ODP_SHARDING_OB_MYSQL);
    }

}
