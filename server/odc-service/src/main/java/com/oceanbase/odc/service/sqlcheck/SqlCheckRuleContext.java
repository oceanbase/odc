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
package com.oceanbase.odc.service.sqlcheck;

import java.util.Map;
import java.util.function.Supplier;

import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Getter;

/**
 * @author longpeng.zlp
 * @date 2025/1/10 10:22
 */
@Getter
public class SqlCheckRuleContext {
    private final Supplier<String> dbVersionSupplier;
    private final DialectType dialectType;
    private final Map<String, Object> parameters;

    private SqlCheckRuleContext(Supplier<String> dbVersionSupplier, DialectType dialectType,
            Map<String, Object> parameters) {
        this.dbVersionSupplier = dbVersionSupplier;
        this.dialectType = dialectType;
        this.parameters = parameters;
    }

    public static SqlCheckRuleContext create(Supplier<String> dbVersionSupplier, DialectType dialectType,
            Map<String, Object> parameters) {
        if (null == dialectType) {
            throw new RuntimeException("dbVersion or dialectType should not be null");
        }
        return new SqlCheckRuleContext(dbVersionSupplier, dialectType, parameters);
    }
}
