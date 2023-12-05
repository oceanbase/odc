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
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;
import com.oceanbase.tools.sqlparser.statement.createtable.TableOptions;

import lombok.NonNull;

/**
 * {@link MySQLRestrictTableCharset}
 *
 * @author yh263208
 * @date 2023-06-19 11:46
 * @since ODC_release_4.2.0
 */
public class MySQLRestrictTableCharset implements SqlCheckRule {

    private final Set<String> allowedCharsets;

    public MySQLRestrictTableCharset(@NonNull Set<String> allowedCharsets) {
        this.allowedCharsets = allowedCharsets;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.RESTRICT_TABLE_CHARSET;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        int offset = context.getStatementOffset(statement);
        if (statement instanceof CreateTable) {
            TableOptions options = ((CreateTable) statement).getTableOptions();
            if (options != null && options.getCharset() != null && !containsIgnoreCase(options.getCharset())) {
                String charsets = "N/A";
                if (CollectionUtils.isNotEmpty(allowedCharsets)) {
                    charsets = String.join(",", allowedCharsets);
                }
                return Collections.singletonList(SqlCheckUtil.buildViolation(statement.getText(), options,
                        getType(), offset, new Object[] {options.getCharset(), charsets}));
            }
        } else if (statement instanceof AlterTable) {
            return ((AlterTable) statement).getAlterTableActions().stream()
                    .filter(a -> a.getCharset() != null && !containsIgnoreCase(a.getCharset()))
                    .map(a -> SqlCheckUtil.buildViolation(statement.getText(), a, getType(), offset,
                            new Object[] {a.getCharset(), String.join(",", allowedCharsets)}))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.MYSQL, DialectType.OB_MYSQL, DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private boolean containsIgnoreCase(String name) {
        String str = StringUtils.unwrap(name, "'");
        str = StringUtils.unwrap(str, "`");
        final String string = StringUtils.unwrap(str, "\"");
        return this.allowedCharsets.stream().anyMatch(s -> StringUtils.equalsIgnoreCase(s, string));
    }

}
