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

import javax.annotation.Nullable;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRule;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRuleContext;
import com.oceanbase.odc.service.sqlcheck.SqlCheckRuleFactory;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckRuleType;
import com.oceanbase.odc.service.sqlcheck.rule.MySQLCheckRationalityForDBObjects;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/6/13 10:03
 * @since: 4.3.4
 */
public class CheckRationalityForDBObjectsFactory implements SqlCheckRuleFactory {
    @Override
    public SqlCheckRuleType getSupportsType() {
        return SqlCheckRuleType.CHECK_RATIONALITY_FOR_DB_OBJECTS;
    }

    @Nullable
    @Override
    public SqlCheckRule generate(@NonNull SqlCheckRuleContext sqlCheckRuleContext) {
        DialectType dialectType = sqlCheckRuleContext.getDialectType();
        if (dialectType == DialectType.OB_MYSQL) {
            return new MySQLCheckRationalityForDBObjects();
        }
        return null;
    }
}
