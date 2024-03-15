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
package com.oceanbase.odc.service.rollbackplan.oracle;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.rollbackplan.oboracle.OBOracleUpdateRollbackGenerator;
import com.oceanbase.tools.sqlparser.statement.update.Update;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/2/23
 * @since ODC_release_4.2.4
 */
public class OracleUpdateRollbackGenerator extends OBOracleUpdateRollbackGenerator {
    public OracleUpdateRollbackGenerator(@NonNull String sql,
            @NonNull Update update,
            @NonNull JdbcOperations jdbcOperations,
            @NonNull RollbackProperties rollbackProperties,
            Long timeOutMilliSeconds) {
        super(sql, update, jdbcOperations, rollbackProperties, timeOutMilliSeconds);
    }

    @Override
    protected DialectType getDialectType() {
        return DialectType.ORACLE;
    }

    @Override
    protected boolean needRemoveDelimiter() {
        return true;
    }
}
