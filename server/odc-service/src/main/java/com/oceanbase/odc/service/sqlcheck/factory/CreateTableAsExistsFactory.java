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
import com.oceanbase.odc.service.sqlcheck.rule.CreateTableAsExists;

import lombok.NonNull;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/1/8 20:33
 * @since: 4.3.3
 */
public class CreateTableAsExistsFactory implements SqlCheckRuleFactory {
    @Override
    public SqlCheckRuleType getSupportsType() {
        return SqlCheckRuleType.CREATE_TABLE_AS_EXISTS;
    }

    @Override
    public SqlCheckRule generate(@NonNull DialectType dialectType, Map<String, Object> parameters) {
        return new CreateTableAsExists();
    }
}
