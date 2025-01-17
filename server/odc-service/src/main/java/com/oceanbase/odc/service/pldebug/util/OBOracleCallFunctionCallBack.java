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
package com.oceanbase.odc.service.pldebug.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.sql.util.DBPLObjectUtil;
import com.oceanbase.odc.core.sql.util.JdbcDataTypeUtil;
import com.oceanbase.odc.service.pldebug.model.PLDebugODPSpecifiedRoute;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.util.OracleSqlBuilder;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link OBOracleCallFunctionCallBack}
 *
 * @author yh263208
 * @date 2023-03-02 16:07
 * @since ODC_release_4.1.2
 * @see ConnectionCallback
 */
public class OBOracleCallFunctionCallBack implements ConnectionCallback<DBFunction> {

    private final DBFunction function;
    private final int timeoutSeconds;

    private final PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute;

    public OBOracleCallFunctionCallBack(@NonNull DBFunction function, int timeoutSeconds) {
        Validate.notBlank(function.getFunName(), "Function name can not be blank");
        DBPLObjectUtil.checkParams(function);
        this.function = function;
        this.timeoutSeconds = timeoutSeconds;
        this.plDebugODPSpecifiedRoute = null;
    }

    public OBOracleCallFunctionCallBack(@NonNull DBFunction function, int timeoutSeconds,
            @NonNull PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute) {
        Validate.notBlank(function.getFunName(), "Function name can not be blank");
        DBPLObjectUtil.checkParams(function);
        this.function = function;
        this.timeoutSeconds = timeoutSeconds;
        this.plDebugODPSpecifiedRoute = plDebugODPSpecifiedRoute;
    }

    @Override
    public DBFunction doInConnection(Connection con) throws SQLException, DataAccessException {
        SqlBuilder sqlBuilder = new OracleSqlBuilder();
        sqlBuilder.append(PLUtils.getSpecifiedRoute(plDebugODPSpecifiedRoute));
        // oracle mode 支持输出参数，因此通过jdbc调用，则需要转换为call procedure
        List<DBPLParam> params = new ArrayList<>();
        if (function.getParams() != null) {
            params = function.getParams();
        }
        if (hasOutParameter()) {
            String plName = "PL_WRAPPER_" + function.getFunName();
            sqlBuilder.append("CREATE OR REPLACE PROCEDURE ").append(plName).append("(");
            params.forEach(p -> {
                String inOutMode = DBPLObjectUtil.getOracleParamString(p.getParamMode());
                sqlBuilder.append(p.getParamName()).space()
                        .append(inOutMode).space()
                        .append(p.getDataType()).append(",");
            });
            sqlBuilder.append("RETURN_VALUE OUT ")
                    .append(function.getReturnType()).append(") IS BEGIN ")
                    .append("RETURN_VALUE:=");
            if (StringUtils.isNotBlank(function.getPackageName())) {
                sqlBuilder.identifier(function.getPackageName()).append(".");
            }
            sqlBuilder.identifier(function.getFunName())
                    .append("(")
                    .append(params.stream()
                            .map(DBPLParam::getParamName)
                            .collect(Collectors.joining(", ")))
                    .append("); END;");
            try (Statement stmt = con.createStatement()) {
                stmt.execute(sqlBuilder.toString());
            }
            try {
                DBProcedure proc = new DBProcedure();
                proc.setProName(plName);
                DBPLParam p = new DBPLParam();
                p.setParamName("RETURN_VALUE");
                p.setParamMode(DBPLParamMode.OUT);
                p.setDataType(function.getReturnType());
                params.add(p);
                proc.setParams(params);
                CallProcedureCallBack callBack;
                if (this.plDebugODPSpecifiedRoute == null) {
                    callBack =
                            new CallProcedureCallBack(proc, timeoutSeconds, new OracleSqlBuilder());
                } else {
                    callBack =
                            new CallProcedureCallBack(proc, timeoutSeconds, new OracleSqlBuilder(),
                                    this.plDebugODPSpecifiedRoute);
                }
                List<DBPLParam> callResult = callBack.doInConnection(con);
                if (CollectionUtils.isEmpty(callResult)) {
                    return function;
                }
                DBPLParam ret = callResult.get(callResult.size() - 1);
                function.setReturnValue(ret.getDefaultValue());
                callResult.remove(callResult.size() - 1);
                function.setParams(callResult);
                return function;
            } finally {
                try (Statement stmt = con.createStatement()) {
                    stmt.execute(PLUtils.getSpecifiedRoute(plDebugODPSpecifiedRoute) + "DROP PROCEDURE " + plName);
                }
            }
        }
        sqlBuilder.append("SELECT ");
        if (StringUtils.isNotBlank(function.getPackageName())) {
            sqlBuilder.identifier(function.getPackageName()).append(".");
        }
        sqlBuilder.identifier(function.getFunName()).append("(");
        String paramStr = params.stream().map(p -> {
            SqlBuilder builder = new OracleSqlBuilder();
            builder.identifier(p.getParamName()).append("=>");
            String value = p.getDefaultValue();
            if (StringUtils.isNotBlank(value)) {
                builder.value(value);
            } else {
                builder.append("null");
            }
            return builder.toString();
        }).collect(Collectors.joining(","));
        sqlBuilder.append(paramStr).append(") FROM DUAL");
        try (Statement statement = con.createStatement()) {
            if (this.timeoutSeconds > 0) {
                statement.setQueryTimeout(this.timeoutSeconds);
            }
            try (ResultSet result = statement.executeQuery(sqlBuilder.toString())) {
                if (result.next()) {
                    Object value = JdbcDataTypeUtil.getValueFromResultSet(result, 1, function.getReturnType());
                    function.setReturnValue(String.valueOf(value));
                }
            }
        }
        function.setParams(null);
        return function;
    }

    private boolean hasOutParameter() {
        List<DBPLParam> params = function.getParams();
        if (CollectionUtils.isEmpty(params)) {
            return false;
        }
        return params.stream().anyMatch(p -> p.getParamMode().isOutParam());
    }

}
