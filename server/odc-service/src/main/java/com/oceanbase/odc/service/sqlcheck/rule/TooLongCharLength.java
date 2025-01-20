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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckUtil;
import com.oceanbase.odc.service.sqlcheck.model.CheckViolation;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.alter.table.AlterTable;
import com.oceanbase.tools.sqlparser.statement.common.CharacterType;
import com.oceanbase.tools.sqlparser.statement.createtable.ColumnDefinition;
import com.oceanbase.tools.sqlparser.statement.createtable.CreateTable;

import lombok.NonNull;

/**
 * {@link TooLongCharLength}
 *
 * @author yh263208
 * @date 2023-06-09 18:18
 * @since ODC_release_4.2.0
 */
public class TooLongCharLength implements SqlCheckRule {

    private final Integer maxCharLength;

    public TooLongCharLength(@NonNull Integer maxCharLength) {
        this.maxCharLength = maxCharLength < 0 ? 0 : maxCharLength;
    }

    @Override
    public SqlCheckRuleType getType() {
        return SqlCheckRuleType.TOO_LONG_CHAR_LENGTN;
    }

    @Override
    public List<CheckViolation> check(@NonNull Statement statement, @NonNull SqlCheckContext context) {
        if (statement instanceof CreateTable) {
            return builds(statement.getText(), ((CreateTable) statement).getColumnDefinitions().stream());
        } else if (statement instanceof AlterTable) {
            return builds(statement.getText(), SqlCheckUtil.fromAlterTable((AlterTable) statement));
        }
        return Collections.emptyList();
    }

    @Override
    public List<DialectType> getSupportsDialectTypes() {
        return Arrays.asList(DialectType.OB_ORACLE, DialectType.OB_MYSQL, DialectType.MYSQL,
                DialectType.ODP_SHARDING_OB_MYSQL);
    }

    private List<CheckViolation> builds(String sql, Stream<ColumnDefinition> stream) {
        return stream.map(ColumnDefinition::getDataType).filter(d -> {
            if (!StringUtils.startsWithIgnoreCase(d.getName(), "char")
                    && !StringUtils.startsWithIgnoreCase(d.getName(), "character")
                    && !StringUtils.startsWithIgnoreCase(d.getName(), "nchar")) {
                return false;
            }
            CharacterType type = (CharacterType) d;
            return type.getLength().compareTo(new BigDecimal(maxCharLength)) > 0;
        }).map(d -> {
            CharacterType type = (CharacterType) d;
            return SqlCheckUtil.buildViolation(sql, d, getType(),
                    new Object[] {maxCharLength, type.getLength().toString()});
        }).collect(Collectors.toList());
    }

}
