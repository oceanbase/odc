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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.jdbc.core.JdbcOperations;

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

    private final Supplier<String> schemaSupplier;

    private final JdbcOperations jdbcOperations;

    public CheckRationalityForDBObjectsFactory(Supplier<String> schemaSupplier, JdbcOperations jdbcOperations) {
        this.schemaSupplier = schemaSupplier;
        this.jdbcOperations = jdbcOperations;
    }

    @Override
    public SqlCheckRuleType getSupportsType() {
        return SqlCheckRuleType.CHECK_RATIONALITY_FOR_DB_OBJECTS;
    }

    @Nullable
    @Override
    public SqlCheckRule generate(@NonNull SqlCheckRuleContext sqlCheckRuleContext) {
        DialectType dialectType = sqlCheckRuleContext.getDialectType();
        Map<String, Object> parameters = sqlCheckRuleContext.getParameters();
        if(parameters == null){
            return null;
        }
        String allowedDBObjectTypesKey = getParameterNameKey("allowed-db-object-types");
        String supportedSimulationKey = getParameterNameKey("simulate-real-execution-scenarios");
        if(parameters.get(allowedDBObjectTypesKey) == null || parameters.get(supportedSimulationKey) == null){
            return null;
        }
        Set<String> allowedDBObjectTypes = new HashSet((List<String>)parameters.get(allowedDBObjectTypesKey));
        Boolean supportedSimulation = Boolean.valueOf(parameters.get(supportedSimulationKey).toString());
        if (dialectType == DialectType.OB_MYSQL) {
            return new MySQLCheckRationalityForDBObjects(supportedSimulation,allowedDBObjectTypes,schemaSupplier, jdbcOperations);
        }
        return null;
    }
}
