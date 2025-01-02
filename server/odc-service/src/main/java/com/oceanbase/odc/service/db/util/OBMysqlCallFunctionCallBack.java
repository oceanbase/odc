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
package com.oceanbase.odc.service.db.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.sql.execute.mapper.JdbcRowMapper;
import com.oceanbase.odc.core.sql.execute.model.JdbcQueryResult;
import com.oceanbase.odc.service.db.model.CallFunctionReq;
import com.oceanbase.odc.service.db.model.CallFunctionResp;
import com.oceanbase.odc.service.db.model.PLOutParam;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.util.MySQLSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

public class OBMysqlCallFunctionCallBack implements ConnectionCallback<CallFunctionResp> {

    private final DBFunction function;
    private final int timeoutSeconds;
    private final JdbcRowMapper rowDataMapper;

    public OBMysqlCallFunctionCallBack(@NonNull CallFunctionReq callFunctionReq, int timeoutSeconds,
            @NonNull JdbcRowMapper rowDataMapper) {
        Validate.notBlank(callFunctionReq.getFunction().getFunName(), "Function name can not be blank");
        this.function = callFunctionReq.getFunction();
        this.timeoutSeconds = timeoutSeconds;
        this.rowDataMapper = rowDataMapper;
    }

    @Override
    public CallFunctionResp doInConnection(Connection con) throws SQLException, DataAccessException {
        List<DBPLParam> params = new ArrayList<>();
        if (function.getParams() != null) {
            params = function.getParams();
        }
        SqlBuilder sqlBuilder = new MySQLSqlBuilder();
        sqlBuilder.append("select ")
                .identifier(function.getFunName())
                .append("(");
        String paramStr = params.stream().map(p -> {
            String value = p.getDefaultValue();
            if (StringUtils.isNotBlank(value)) {
                return StringUtils.quoteMysqlValue(value);
            } else if (Objects.isNull(value)) {
                return "null";
            }
            return "''";
        }).collect(Collectors.joining(", "));
        sqlBuilder.append(paramStr).append(")");
        try (Statement stmt = con.createStatement()) {
            if (this.timeoutSeconds > 0) {
                stmt.setQueryTimeout(this.timeoutSeconds);
            }
            try (ResultSet res = stmt.executeQuery(sqlBuilder.toString())) {
                if (!res.next()) {
                    return generateDefaultReturnValue();
                }
                JdbcQueryResult jdbcQueryResult = new JdbcQueryResult(res.getMetaData(), rowDataMapper);
                jdbcQueryResult.addLine(res);
                List<List<Object>> rows = jdbcQueryResult.getRows();
                if (CollectionUtils.size(rows) == 1 && CollectionUtils.size(rows.get(0)) == 1) {
                    CallFunctionResp callFunctionResp = new CallFunctionResp();
                    PLOutParam plOutParam = new PLOutParam();
                    plOutParam.setValue(String.valueOf(rows.get(0).get(0)));
                    plOutParam.setDataType(function.getReturnType());
                    callFunctionResp.setReturnValue(plOutParam);
                    callFunctionResp.setOutParams(null);
                    return callFunctionResp;
                }
                throw new IllegalStateException("The return value of a function must be unique");
            }
        } catch (Exception e) {
            CallFunctionResp callFunctionResp = generateDefaultReturnValue();
            callFunctionResp.setErrorMessage(e.getMessage());
            return callFunctionResp;
        }
    }

    private CallFunctionResp generateDefaultReturnValue() {
        CallFunctionResp callFunctionResp = new CallFunctionResp();
        PLOutParam plOutParam = new PLOutParam();
        plOutParam.setValue(function.getReturnValue());
        plOutParam.setDataType(function.getReturnType());
        callFunctionResp.setReturnValue(plOutParam);
        return callFunctionResp;
    }
}
