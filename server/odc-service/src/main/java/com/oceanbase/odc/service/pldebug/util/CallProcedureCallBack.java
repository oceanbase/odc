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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.sql.util.DBPLObjectUtil;
import com.oceanbase.odc.core.sql.util.JdbcDataTypeUtil;
import com.oceanbase.odc.service.pldebug.model.PLDebugODPSpecifiedRoute;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

import lombok.NonNull;

/**
 * {@link CallProcedureCallBack}
 *
 * @author yh263208
 * @date 2023-03-02 14:50
 * @since ODC_release_4.1.2
 */
public class CallProcedureCallBack implements ConnectionCallback<List<DBPLParam>> {

    private final DBProcedure procedure;
    private final SqlBuilder sqlBuilder;
    private final int timeoutSeconds;
    private final PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute;

    public CallProcedureCallBack(@NonNull DBProcedure procedure,
            int timeoutSeconds, @NonNull SqlBuilder sqlBuilder) {
        Validate.notBlank(procedure.getProName(), "Procedure name can not be blank");
        DBPLObjectUtil.checkParams(procedure);
        this.procedure = procedure;
        this.sqlBuilder = sqlBuilder;
        this.timeoutSeconds = timeoutSeconds;
        this.plDebugODPSpecifiedRoute = null;
    }

    public CallProcedureCallBack(@NonNull DBProcedure procedure,
            int timeoutSeconds, @NonNull SqlBuilder sqlBuilder,
            @NonNull PLDebugODPSpecifiedRoute plDebugODPSpecifiedRoute) {
        Validate.notBlank(procedure.getProName(), "Procedure name can not be blank");
        DBPLObjectUtil.checkParams(procedure);
        this.procedure = procedure;
        this.sqlBuilder = sqlBuilder;
        this.timeoutSeconds = timeoutSeconds;
        this.plDebugODPSpecifiedRoute = plDebugODPSpecifiedRoute;
    }

    @Override
    public List<DBPLParam> doInConnection(Connection con) throws SQLException, DataAccessException {
        sqlBuilder.append(PLUtils.getSpecifiedRoute(this.plDebugODPSpecifiedRoute));
        sqlBuilder.append("CALL ");
        if (StringUtils.isNotBlank(procedure.getPackageName())) {
            sqlBuilder.identifier(procedure.getPackageName()).append(".");
        }
        List<DBPLParam> plParams = new ArrayList<>();
        if (procedure.getParams() != null) {
            plParams = procedure.getParams();
        }
        sqlBuilder.identifier(procedure.getProName()).append("(");
        String paramStr = plParams.stream().map(dbplParam -> "?").collect(Collectors.joining(", "));
        sqlBuilder.append(paramStr).append(")");
        List<DBPLParam> result = new ArrayList<>();
        try (CallableStatement stmt = con.prepareCall("{" + sqlBuilder.toString() + "}")) {
            if (this.timeoutSeconds > 0) {
                stmt.setQueryTimeout(this.timeoutSeconds);
            }
            for (int i = 0; i < plParams.size(); i++) {
                DBPLParam param = plParams.get(i);
                DBPLParamMode type = param.getParamMode();
                String dataType = param.getDataType();
                if (type == DBPLParamMode.IN
                        || type == DBPLParamMode.INOUT) {
                    try {
                        JdbcDataTypeUtil.setValueIntoStatement(stmt, i + 1,
                                dataType, param.getDefaultValue());
                    } catch (Exception e) {
                        throw new BadArgumentException(ErrorCodes.ArgumentValueAndTypeMismatched,
                                new Object[] {param.getParamName(), param.getDefaultValue(), dataType}, null);
                    }
                }
                if (type == DBPLParamMode.OUT
                        || type == DBPLParamMode.INOUT) {
                    stmt.registerOutParameter(i + 1,
                            JdbcDataTypeUtil.parseDataType(dataType).getVendorTypeNumber());
                }
            }
            // root用户 alter system set _ob_enable_prepared_statement = true;
            stmt.executeQuery();
            // 获取输出参数
            for (int i = 0; i < plParams.size(); i++) {
                DBPLParam param = plParams.get(i);
                DBPLParamMode type = param.getParamMode();
                if (type != DBPLParamMode.OUT
                        && type != DBPLParamMode.INOUT) {
                    continue;
                }
                Object value = JdbcDataTypeUtil.getValueFromStatement(
                        stmt, i + 1, param.getDataType());
                DBPLParam item = new DBPLParam();
                item.setDefaultValue(value == null ? null : String.valueOf(value));
                item.setParamName(param.getParamName());
                item.setParamMode(param.getParamMode());
                item.setDataType(param.getDataType());
                result.add(item);
            }
        }
        return result;
    }

}
