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
package com.oceanbase.odc.service.sqlcheck.factory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRuleFactory;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLMissingRequiredColumns;
import com.oceanbase.odc.service.sqlcheck.rule.OracleMissingRequiredColumns;

import lombok.NonNull;

public class MissingRequiredColumnsFactory implements SqlCheckRuleFactory {

    @Override
    public SqlCheckRuleType getSupportsType() {
        return SqlCheckRuleType.MISSING_REQUIRED_COLUMNS;
    }

    @Override
    @SuppressWarnings("all")
    public SqlCheckRule generate(@NonNull DialectType dialectType, Map<String, Object> parameters) {
        String key = getParameterNameKey("column-names");
        Set<String> cols;
        if (parameters == null || parameters.isEmpty() || parameters.get(key) == null) {
            cols = new HashSet<>();
        } else {
            cols = new HashSet<>((List<String>) parameters.get(key));
        }
        return dialectType.isMysql() ? new MySQLMissingRequiredColumns(cols) : new OracleMissingRequiredColumns(cols);
    }

}
