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
package com.oceanbase.odc.service.rollbackplan;

import java.io.StringReader;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.execute.SyncJdbcExecutor;
import com.oceanbase.odc.service.rollbackplan.model.RollbackProperties;
import com.oceanbase.odc.service.rollbackplan.obmysql.OBMySqlDeleteRollbackGenerator;
import com.oceanbase.odc.service.rollbackplan.obmysql.OBMySqlUpdateRollbackGenerator;
import com.oceanbase.odc.service.rollbackplan.oboracle.OBOracleDeleteRollbackGenerator;
import com.oceanbase.odc.service.rollbackplan.oboracle.OBOracleUpdateRollbackGenerator;
import com.oceanbase.odc.service.rollbackplan.oracle.OracleDeleteRollbackGenerator;
import com.oceanbase.odc.service.rollbackplan.oracle.OracleUpdateRollbackGenerator;
import com.oceanbase.tools.sqlparser.OBMySQLParser;
import com.oceanbase.tools.sqlparser.OBOracleSQLParser;
import com.oceanbase.tools.sqlparser.statement.Statement;
import com.oceanbase.tools.sqlparser.statement.delete.Delete;
import com.oceanbase.tools.sqlparser.statement.update.Update;

import lombok.NonNull;

/**
 * {@link RollbackGeneratorFactory}
 *
 * @author jingtian
 * @date 2023/5/16
 * @since ODC_release_4.2.0
 */
public class RollbackGeneratorFactory {

    public static GenerateRollbackPlan create(@NonNull String sql, @NonNull RollbackProperties rollbackProperties,
            @NonNull ConnectionSession connectionSession, Long timeOutMilliSeconds) {
        ConnectType connectType = connectionSession.getConnectType();
        Validate.notNull(connectType, "ConnectType can not be null");
        SyncJdbcExecutor syncJdbcExecutor =
                connectionSession.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);

        Statement statement;
        if (connectType.getDialectType().isMysql()) {
            statement = new OBMySQLParser().parse(new StringReader(sql));
        } else if (connectType.getDialectType().isOracle()) {
            statement = new OBOracleSQLParser().parse(new StringReader(sql));
        } else {
            throw new UnsupportedOperationException("Unsupported dialect type: " + connectType.getDialectType());
        }

        return getRollbackPlan(connectType.getDialectType(), sql, statement, syncJdbcExecutor,
                rollbackProperties, timeOutMilliSeconds);
    }

    private static GenerateRollbackPlan getRollbackPlan(DialectType dialectType, String sql, Statement statement,
            SyncJdbcExecutor syncJdbcExecutor, RollbackProperties rollbackProperties, Long timeOutMilliSeconds) {
        switch (dialectType) {
            case MYSQL:
            case OB_MYSQL:
            case ODP_SHARDING_OB_MYSQL:
                if (statement instanceof Update) {
                    return new OBMySqlUpdateRollbackGenerator(sql, (Update) statement, syncJdbcExecutor,
                            rollbackProperties, timeOutMilliSeconds);
                } else if (statement instanceof Delete) {
                    return new OBMySqlDeleteRollbackGenerator(sql, (Delete) statement, syncJdbcExecutor,
                            rollbackProperties, timeOutMilliSeconds);
                } else {
                    throw new UnsupportedSqlTypeForRollbackPlanException("Unsupported sql type, sql: " + sql);
                }
            case OB_ORACLE:
                if (statement instanceof Update) {
                    return new OBOracleUpdateRollbackGenerator(sql, (Update) statement, syncJdbcExecutor,
                            rollbackProperties, timeOutMilliSeconds);
                } else if (statement instanceof Delete) {
                    return new OBOracleDeleteRollbackGenerator(sql, (Delete) statement, syncJdbcExecutor,
                            rollbackProperties, timeOutMilliSeconds);
                } else {
                    throw new UnsupportedSqlTypeForRollbackPlanException("Unsupported sql type, sql: " + sql);
                }
            case ORACLE:
                if (statement instanceof Update) {
                    return new OracleUpdateRollbackGenerator(sql, (Update) statement, syncJdbcExecutor,
                            rollbackProperties, timeOutMilliSeconds);
                } else if (statement instanceof Delete) {
                    return new OracleDeleteRollbackGenerator(sql, (Delete) statement, syncJdbcExecutor,
                            rollbackProperties, timeOutMilliSeconds);
                } else {
                    throw new UnsupportedSqlTypeForRollbackPlanException("Unsupported sql type, sql: " + sql);
                }
            default:
                throw new UnsupportedOperationException("Unsupported dialect type: " + dialectType);
        }
    }

}
