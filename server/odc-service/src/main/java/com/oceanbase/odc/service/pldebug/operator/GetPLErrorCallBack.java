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
package com.oceanbase.odc.service.pldebug.operator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.lang.Nullable;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.pldebug.model.DBPLError;
import com.oceanbase.odc.service.pldebug.model.PLDebugODPSpecifiedRoute;
import com.oceanbase.odc.service.pldebug.util.PLUtils;

/**
 * @author yaobin
 * @date 2023-02-19
 * @since 4.2.0
 */
public class GetPLErrorCallBack implements ConnectionCallback<List<DBPLError>> {
    private final ConnectionConfig connectionConfig;
    private final DBPLError dbplError;
    private final PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute;

    public GetPLErrorCallBack(ConnectionConfig connectionConfig, DBPLError dbplError,
            PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute) {
        this.connectionConfig = connectionConfig;
        this.dbplError = dbplError;
        this.plDebugODPSpecifiedRoute = plDebugODPSpecifiedRoute;
    }

    @Override
    public List<DBPLError> doInConnection(@Nullable Connection connection) throws DataAccessException {
        // validate
        PreConditions.notNull(dbplError, "plError");
        PreConditions.notBlank(dbplError.getName(), "plError.name");
        PreConditions.notBlank(dbplError.getType(), "plError.type");

        String tableName = "all_errors";
        if (PLUtils.isSys(connectionConfig)) {
            tableName = "dba_errors";
        }
        String schema = connectionConfig.getDefaultSchema();
        String specifiedRoute = PLUtils.getSpecifiedRoute(this.plDebugODPSpecifiedRoute);
        String sql = String.format(
                "%s select * from %s WHERE type = '%s' and name = '%s' and owner = '%s' order by line asc;",
                specifiedRoute,
                tableName, dbplError.getType(), dbplError.getName(), schema);
        List<DBPLError> dbplErrors = new ArrayList<>();

        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                DBPLError error = new DBPLError();
                error.setName(rs.getString(1));
                error.setType(rs.getString(2));
                error.setLine(rs.getInt(4));
                error.setPosition(rs.getInt(5));
                error.setText(rs.getString(6));
                error.setAttribute(rs.getString(7));
                error.setMessageNumber(rs.getInt(8));
                dbplErrors.add(error);
            }

        } catch (SQLException exception) {
            throw new OBException(ErrorCodes.ObExecuteSqlFailed,
                    new Object[] {exception.getMessage()}, "get PLErrors occur error",
                    HttpStatus.BAD_REQUEST);
        }
        return dbplErrors;
    }
}
