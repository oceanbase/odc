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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRuleFactory;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLRestrictAutoIncrementDataTypes;

import lombok.NonNull;

public class RestrictAutoIncrementDataTypesFactory implements SqlCheckRuleFactory {

    @Override
    public SqlCheckRuleType getSupportsType() {
        return SqlCheckRuleType.RESTRICT_AUTO_INCREMENT_DATATYPES;
    }

    @Override
    @SuppressWarnings("all")
    public SqlCheckRule generate(@NonNull DialectType dialectType, Map<String, Object> parameters) {
        String key = getParameterNameKey("allowed-datatypes");
        Set<String> types;
        if (parameters == null || parameters.isEmpty() || parameters.get(key) == null) {
            types = new HashSet<>(Collections.singletonList("bigint"));
        } else {
            types = new HashSet<>((List<String>) parameters.get(key));
        }
        return dialectType.isMysql() ? new MySQLRestrictAutoIncrementDataTypes(types) : null;
    }

}
