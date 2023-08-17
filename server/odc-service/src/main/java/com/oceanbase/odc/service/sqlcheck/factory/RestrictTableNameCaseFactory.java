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

import java.util.Map;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRuleFactory;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.OracleRestrictTableNameCase;

import lombok.NonNull;

public class RestrictTableNameCaseFactory implements SqlCheckRuleFactory {

    @Override
    public SqlCheckRuleType getSupportsType() {
        return SqlCheckRuleType.RESTRICT_TABLE_NAME_CASE;
    }

    @Override
    public SqlCheckRule generate(@NonNull DialectType dialectType, Map<String, Object> parameters) {
        Boolean lowercase = null;
        Boolean uppercase = null;
        if (parameters != null && !parameters.isEmpty()) {
            String upperKey = getParameterNameKey("is-uppercase");
            String lowerKey = getParameterNameKey("is-lowercase");
            if (parameters.get(upperKey) != null) {
                uppercase = Boolean.valueOf(parameters.get(upperKey).toString());
            }
            if (parameters.get(lowerKey) != null) {
                lowercase = Boolean.valueOf(parameters.get(lowerKey).toString());
            }
        }
        return dialectType == DialectType.OB_ORACLE ? new OracleRestrictTableNameCase(lowercase, uppercase) : null;
    }

}
