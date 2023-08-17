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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.core.sql.util.JdbcDataTypeUtil;
import com.oceanbase.odc.service.db.model.AnonymousBlockFunctionCall;
import com.oceanbase.odc.service.db.model.CallFunctionReq;
import com.oceanbase.odc.service.db.model.CallFunctionResp;
import com.oceanbase.odc.service.db.model.CursorResultSet;
import com.oceanbase.odc.service.db.model.OdcDBTableColumn;
import com.oceanbase.odc.service.db.model.PLOutParam;
import com.oceanbase.odc.service.db.model.PLParameter;
import com.oceanbase.odc.service.db.model.PLVariable;
import com.oceanbase.odc.service.db.parser.AnonymousBlockParser;
import com.oceanbase.odc.service.db.parser.result.ParserCallPLByAnonymousBlockResult;
import com.oceanbase.odc.service.session.model.DBResultSetMetaData;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;

/**
 * {@link OBOracleCallFunctionBlockCallBack}
 */
public class OBOracleCallFunctionBlockCallBack implements ConnectionCallback<CallFunctionResp> {

    private final CallFunctionReq callFunctionReq;
    private final int timeoutSeconds;

    public OBOracleCallFunctionBlockCallBack(@NonNull CallFunctionReq callFunctionReq, int timeoutSeconds) {
        Validate.notBlank(callFunctionReq.getFunction().getFunName(), "Function name can not be blank");
        this.callFunctionReq = callFunctionReq;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public CallFunctionResp doInConnection(Connection con) throws SQLException, DataAccessException {
        CallFunctionResp callFunctionResp = new CallFunctionResp();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(callFunctionReq.getAnonymousBlockDdl());
        List<DBPLParam> plParams = new ArrayList<>();
        if (callFunctionReq.getFunction().getParams() != null) {
            plParams = callFunctionReq.getFunction().getParams();
        }
        ParserCallPLByAnonymousBlockResult result =
                AnonymousBlockParser.parserCallPLAnonymousBlockResult(callFunctionReq.getAnonymousBlockDdl(), 0);
        Map<String, AnonymousBlockFunctionCall> functionCallMap = result.getFunctionCallMap();
        AnonymousBlockFunctionCall functionCall = functionCallMap.get(callFunctionReq.getFunction().getFunName());
        if (functionCall == null) {
            throw new BadArgumentException(ErrorCodes.ExecPLByAnonymousBlockErrorCallStatement,
                    new Object[] {callFunctionReq.getFunction().getFunName()}, null);
        }
        Map<String, PLVariable> variablesMap = result.getVariablesMap();
        Map<String, PLParameter> outParameter = result.getOutParameter();
        Map<String, PLParameter> params = functionCall.getParams();
        PLParameter returnParam = functionCall.getReturnParam();

        int changeIndexValue = 0;
        if (returnParam != null) {
            if (!callFunctionReq.getFunction().isReturnExtendedType()
                    || "sys_refcursor".equalsIgnoreCase(callFunctionReq.getFunction().getReturnType())) {
                stringBuilder.replace(returnParam.getBeginIndex(), returnParam.getEndIndex() + 1, "?");
                changeIndexValue = returnParam.getParamName().length() - 1;
            }
        } else {
            throw new BadArgumentException(ErrorCodes.ExecPLByAnonymousBlockErrorCallStatement,
                    new Object[] {callFunctionReq.getFunction().getFunName()}, null);
        }
        for (DBPLParam param : plParams) {
            PLParameter anonymousBlockParam = params.get(param.getParamName());
            if (anonymousBlockParam == null) {
                throw new BadArgumentException(ErrorCodes.ExecPLByAnonymousBlockUndefinedParameters,
                        new Object[] {functionCall.getCallLine(), param.getParamName()}, null);
            }
            int begin = anonymousBlockParam.getBeginIndex();
            int end = anonymousBlockParam.getEndIndex();
            stringBuilder.replace(begin - changeIndexValue, end + 1 - changeIndexValue, param.getParamName());
            changeIndexValue += end - begin - param.getParamName().length() + 1;
        }
        for (DBPLParam param : plParams) {
            if (param.getParamMode() == DBPLParamMode.INOUT || param.getParamMode() == DBPLParamMode.OUT) {
                if (!param.isExtendedType() || "sys_refcursor".equalsIgnoreCase(param.getDataType())) {
                    if (outParameter != null && outParameter.get(param.getParamName()) != null
                            && variablesMap.get(param.getParamName()) != null) {
                        PLParameter plParameter = outParameter.get(param.getParamName());
                        int begin = plParameter.getBeginIndex();
                        int end = plParameter.getEndIndex();
                        stringBuilder.replace(begin - changeIndexValue, end + 1 - changeIndexValue, "?");
                        changeIndexValue += end - begin;
                    }
                }
            }
        }
        int paramIndex = 0;
        try (CallableStatement stmt = con.prepareCall(stringBuilder.toString())) {
            if (this.timeoutSeconds > 0) {
                stmt.setQueryTimeout(this.timeoutSeconds);
            }
            if (!callFunctionReq.getFunction().isReturnExtendedType()
                    || "sys_refcursor".equalsIgnoreCase(callFunctionReq.getFunction().getReturnType())) {
                String returnType = callFunctionReq.getFunction().getReturnType();
                stmt.registerOutParameter(paramIndex + 1,
                        JdbcDataTypeUtil.parseDataType(returnType).getVendorTypeNumber());
                paramIndex++;
            }
            for (DBPLParam param : plParams) {
                DBPLParamMode type = param.getParamMode();
                String dataType = param.getDataType();
                if (type == DBPLParamMode.OUT || type == DBPLParamMode.INOUT) {
                    if (!param.isExtendedType() || "sys_refcursor".equalsIgnoreCase(param.getDataType())) {
                        if (outParameter != null && outParameter.get(param.getParamName()) != null
                                && variablesMap.get(param.getParamName()) != null) {
                            stmt.registerOutParameter(paramIndex + 1,
                                    JdbcDataTypeUtil.parseDataType(dataType).getVendorTypeNumber());
                            paramIndex++;
                        }
                    }
                }
            }
            stmt.executeQuery();
            // 获取输出参数
            paramIndex = 0;
            List<PLOutParam> outParams = new ArrayList<>();
            DBPLParam dbplParam = new DBPLParam();
            dbplParam.setDataType(callFunctionReq.getFunction().getReturnType());
            dbplParam.setParamMode(DBPLParamMode.OUT);
            dbplParam.setExtendedType(callFunctionReq.getFunction().isReturnExtendedType());
            PLOutParam returnValue = getOutParam(dbplParam, paramIndex, stmt);
            if (!"<Ext>".equals(returnValue.getValue())) {
                paramIndex++;
            }
            callFunctionResp.setReturnValue(returnValue);
            for (DBPLParam param : plParams) {
                DBPLParamMode type = param.getParamMode();
                PLOutParam plOutParam;
                if (type == DBPLParamMode.OUT || type == DBPLParamMode.INOUT) {
                    if (outParameter != null && outParameter.get(param.getParamName()) != null
                            && variablesMap.get(param.getParamName()) != null) {
                        plOutParam = getOutParam(param, paramIndex, stmt);
                        if (!"<Ext>".equals(plOutParam.getValue())) {
                            paramIndex++;
                        }
                        outParams.add(plOutParam);
                    }
                }
            }
            callFunctionResp.setOutParams(outParams);
        }
        return callFunctionResp;
    }

    private PLOutParam getOutParam(DBPLParam param, int paramIndex, CallableStatement stmt) throws SQLException {
        PLOutParam plOutParam = new PLOutParam();
        if (param.isExtendedType()) {
            if ("sys_refcursor".equalsIgnoreCase(param.getDataType())) {
                Object value = JdbcDataTypeUtil.getValueFromStatement(
                        stmt, paramIndex + 1, param.getDataType());
                plOutParam = getCursorResultSet(value);
            } else {
                plOutParam.setValue("<Ext>");
            }
        } else {
            Object value = JdbcDataTypeUtil.getValueFromStatement(
                    stmt, paramIndex + 1, param.getDataType());
            plOutParam.setValue(value == null ? null : String.valueOf(value));
        }
        plOutParam.setParamName(param.getParamName());
        plOutParam.setParamMode(param.getParamMode());
        plOutParam.setDataType(param.getDataType());
        return plOutParam;
    }

    private PLOutParam getCursorResultSet(Object value) throws SQLException {
        PLOutParam plOutParam = new PLOutParam();
        if (value == null) {
            plOutParam.setValue(null);
        } else {
            plOutParam.setValue("<Cursor>");
            CursorResultSet cursorResultSet = new CursorResultSet();
            DBResultSetMetaData dbResultSetMetaData = new DBResultSetMetaData();
            List<DBTableColumn> columnList = new ArrayList<>();
            OdcDBTableColumn odcdbTableColumn = new OdcDBTableColumn();
            ResultSet resultSet = (ResultSet) value;
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            for (int j = 1; j <= resultSetMetaData.getColumnCount(); j++) {
                odcdbTableColumn.setColumnName(resultSetMetaData.getColumnName(j));
                odcdbTableColumn.setTypeName(resultSetMetaData.getColumnTypeName(j));
                columnList.add(odcdbTableColumn);
            }
            dbResultSetMetaData.setColumnList(columnList);
            cursorResultSet.setResultSetMetaData(dbResultSetMetaData);
            List<List<Object>> rowsValue = new ArrayList<>();
            while (resultSet.next()) {// 按行获取值
                List<Object> val = new ArrayList<>();
                for (int j = 1; j <= resultSetMetaData.getColumnCount(); j++) {
                    if ("CLOB".equalsIgnoreCase(resultSetMetaData.getColumnTypeName(j))) {
                        val.add("<CLOB>");
                    } else if ("BLOB".equalsIgnoreCase(resultSetMetaData.getColumnTypeName(j))) {
                        val.add("<BLOB>");
                    } else {
                        val.add(resultSet.getString(j));
                    }
                }
                rowsValue.add(val);
            }
            cursorResultSet.setRows(rowsValue);
            plOutParam.setCursorResultSet(cursorResultSet);
        }
        return plOutParam;
    }
}
