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
package com.oceanbase.odc.service.pldebug.session;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import com.alibaba.fastjson.JSONObject;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.model.AnonymousBlockFunctionCall;
import com.oceanbase.odc.service.db.model.AnonymousBlockProcedureCall;
import com.oceanbase.odc.service.db.model.PLIdentity;
import com.oceanbase.odc.service.db.parser.AnonymousBlockParser;
import com.oceanbase.odc.service.db.parser.result.ParserCallPLByAnonymousBlockResult;
import com.oceanbase.odc.service.pldebug.model.CurrentDebugPLObject;
import com.oceanbase.odc.service.pldebug.model.DBPLError;
import com.oceanbase.odc.service.pldebug.model.PLDebugBreakpoint;
import com.oceanbase.odc.service.pldebug.model.PLDebugErrorCode;
import com.oceanbase.odc.service.pldebug.model.PLDebugPrintBacktrace;
import com.oceanbase.odc.service.pldebug.model.PLDebugResult;
import com.oceanbase.odc.service.pldebug.model.PLDebugStatusReason;
import com.oceanbase.odc.service.pldebug.model.PLDebugVariable;
import com.oceanbase.odc.service.pldebug.model.StartPLDebugReq;
import com.oceanbase.odc.service.pldebug.operator.DBPLOperators;
import com.oceanbase.odc.service.pldebug.operator.GetPLErrorCallBack;
import com.oceanbase.odc.service.pldebug.util.PLUtils;
import com.oceanbase.tools.dbbrowser.model.DBBasicPLObject;
import com.oceanbase.tools.dbbrowser.model.DBFunction;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBPLParam;
import com.oceanbase.tools.dbbrowser.model.DBPLParamMode;
import com.oceanbase.tools.dbbrowser.model.DBPackage;
import com.oceanbase.tools.dbbrowser.model.DBPackageDetail;
import com.oceanbase.tools.dbbrowser.model.DBProcedure;
import com.oceanbase.tools.dbbrowser.model.DBTrigger;
import com.oceanbase.tools.dbbrowser.model.DBType;
import com.oceanbase.tools.dbbrowser.parser.PLParser;
import com.oceanbase.tools.dbbrowser.parser.listener.OracleModeParserListener;
import com.oceanbase.tools.dbbrowser.parser.result.ParseOraclePLResult;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/11/18
 */

@Data
@Slf4j
public class DebuggerSession extends AbstractDebugSession {
    private static final String MIN_OBSERVER_VERSION_SUPPORT_SYNC = "3.2.3.1";

    // for get errors
    private String objectName;
    private DBObjectType debugType;
    private boolean syncEnabled;
    // 程序包对象，只有调试的是程序包下的 PL 或 函数时该字段不为 null
    private Map<PackageKey, DBPackageDetail> debugPackageMap = new HashMap<>();
    private CurrentDebugPLObject currentDebugPLObject;
    private volatile StackInfo currentStackInfo;
    private String ddl;
    private String packageName;
    private DBObjectType plType;
    private String plName;
    private static final int MAX_TRY_STEP_INTO_TIMES = 5;

    public DebuggerSession(DebuggeeSession debuggeeSession, StartPLDebugReq req, boolean syncEnabled)
            throws Exception {
        debugId = debuggeeSession.getDebugId();
        debugType = req.getDebugType();
        ddl = req.getAnonymousBlock();
        this.syncEnabled = syncEnabled;
        if (debuggeeSession.getPlDebugODPSpecifiedRoute() != null) {
            this.plDebugODPSpecifiedRoute = debuggeeSession.getPlDebugODPSpecifiedRoute();
        }
        // Debugger must connect to database host the same as debuggee
        // Set the timeout period, which is measured in microseconds (µs)
        List<String> initSqls = Collections.singletonList(
                String.format("set session ob_query_timeout = %s;", DEBUG_TIMEOUT_MS * 1000));
        acquireNewConnection(debuggeeSession.getConnectionSession(),
                () -> cloneDataSource(debuggeeSession.getNewDataSource(), initSqls));
        try (Statement stmt = connection.createStatement()) {
            // Mount this new session of debugger to the previously initialized debuggee
            stmt.execute(String.format("%s call dbms_debug.attach_session(%s);", PLUtils.getSpecifiedRoute(
                    this.plDebugODPSpecifiedRoute), debuggeeSession.getDebugId()));
        } catch (Exception e) {
            log.warn("Call dbms_debug.attach_session() failed", e);
            throw OBException
                    .executeFailed(String.format("Call dbms_debug.attach_session() failed, reason=%s", e.getMessage()));
        }
        currentStackInfo = new StackInfo();
        currentStackInfo.objectType = req.getDebugType();
        String packageName = null;
        if (req.getProcedure() != null) {
            if (req.getProcedure().getPackageName() != null) {
                packageName = req.getProcedure().getPackageName();
            } else {
                packageName = null;
            }
            if (packageName == null) {
                currentDebugPLObject = parserDdl(req.getProcedure().getDdl());
                currentDebugPLObject.setBasicPlObjectName(req.getProcedure().getProName());
                currentDebugPLObject.setBasicPlObjectType(DBObjectType.PROCEDURE);
            }
            plType = DBObjectType.PROCEDURE;
            plName = req.getProcedure().getProName();
        }
        if (req.getFunction() != null) {
            if (req.getFunction().getPackageName() != null) {
                packageName = req.getFunction().getPackageName();
            } else {
                packageName = null;
            }
            if (packageName == null) {
                currentDebugPLObject = parserDdl(req.getFunction().getDdl());
                currentDebugPLObject.setBasicPlObjectName(req.getFunction().getFunName());
                currentDebugPLObject.setBasicPlObjectType(DBObjectType.FUNCTION);
            }
            plType = DBObjectType.FUNCTION;
            plName = req.getFunction().getFunName();
        }
        objectName = ConnectionSessionUtil.getCurrentSchema(connectionSession) + "__anonymous_block__";
        if (packageName != null) {
            try {
                DBPackageDetail detail = getPackageBodyObject(packageName, null);
                detail.setPackageName(packageName);
                String owner = ConnectionSessionUtil.getCurrentSchema(connectionSession);
                detail.setOwner(owner);
                this.debugPackageMap.put(new PackageKey(owner, packageName), detail);
            } catch (Exception e) {
                log.warn("Failed to get package detail, packageName={}", packageName, e);
            }
        }
        if (!debuggeeSession.detectSessionAlive()) {
            // if debuggee is not alive, do not sync and throw exception
            log.warn("Debuggee session is not alive");
            PLDebugResult result = debuggeeSession.getResult();
            if (result == null || result.getExecutionErrorMessage() == null) {
                throw OBException.executeFailed("Debuggee session is not alive before debugger start");
            }
            throw OBException.executeFailed(result.getExecutionErrorMessage());
        }
        synchronize();
        if (req.getFunction() != null || req.getProcedure() != null) {
            debugBefore();
        }
    }

    private DebugDataSource cloneDataSource(DebugDataSource originDataSource, List<String> initSqls) {
        ConnectionConfig config = (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        DebugDataSource debuggerDataSource = new DebugDataSource(config, initSqls, this.plDebugODPSpecifiedRoute);
        debuggerDataSource.setUrl(originDataSource.getUrl());
        debuggerDataSource.setUsername(originDataSource.getUsername());
        debuggerDataSource.setPassword(originDataSource.getPassword());
        debuggerDataSource.setDriverClassName(OdcConstants.DEFAULT_DRIVER_CLASS_NAME);
        return debuggerDataSource;
    }

    private void debugBefore() {
        List<PLDebugBreakpoint> breakpoints = new ArrayList<>();
        PLDebugBreakpoint beginBreakpoint = new PLDebugBreakpoint();
        beginBreakpoint.setObjectType(DBObjectType.ANONYMOUS_BLOCK);
        beginBreakpoint.setObjectName(objectName);
        ParserCallPLByAnonymousBlockResult result = AnonymousBlockParser.parserCallPLAnonymousBlockResult(ddl, 0);
        if (DBObjectType.PROCEDURE == plType) {
            Map<String, AnonymousBlockProcedureCall> procedureCallMap = result.getProcedureCallMap();
            AnonymousBlockProcedureCall procedureCall = procedureCallMap.get(plName);
            beginBreakpoint.setLineNum(procedureCall.getCallLine());
        }
        if (DBObjectType.FUNCTION == plType) {
            Map<String, AnonymousBlockFunctionCall> functionCallMap = result.getFunctionCallMap();
            AnonymousBlockFunctionCall functionCall = functionCallMap.get(plName);
            beginBreakpoint.setLineNum(functionCall.getCallLine());
        }
        breakpoints.add(beginBreakpoint);
        setBreakpoints(breakpoints);

        resume();
        stepInForStartingDebug();
        currentStackInfo.owner = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        currentStackInfo.objectType = plType;
        objectName = plName;
        debugType = plType;
        getBacktrace();
        currentStackInfo.stackDepth = 1;

    }

    private void synchronize() throws Exception {
        if (VersionUtils.isGreaterThanOrEqualsTo(ConnectionSessionUtil.getVersion(connectionSession),
                MIN_OBSERVER_VERSION_SUPPORT_SYNC)
                && syncEnabled) {
            log.info("PLDebug sync enabled, try to start sync");
            // sync with observer to ensure debug init complete
            DBPLParam resultParam = DBPLParam.of("result", DBPLParamMode.OUT, "BINARY_INTEGER");
            DBPLParam messageParam = DBPLParam.of("message", DBPLParamMode.OUT, "VARCHAR2");
            DBProcedure dbProcedure = DBProcedure.of(OdcConstants.PL_DEBUG_PACKAGE,
                    OdcConstants.PROCEDURE_SYNCHRONIZE, Arrays.asList(resultParam, messageParam));
            List<DBPLParam> result = executeProcedure(dbProcedure);

            int returnVal = Integer.parseInt(result.get(0).getDefaultValue());
            if (PLDebugErrorCode.success.getId() != returnVal) {
                PLDebugErrorCode code = PLDebugErrorCode.getEnumById(returnVal);
                throw OBException.executeFailed(ErrorCodes.DebugStartFailed,
                        String.format("Failed to sync with debuggee, return code: %s", code));
            }
            log.info("PLDebug sync end successfully");
        }
    }

    @Override
    public boolean detectSessionAlive() {
        boolean alive = false;
        try (Statement stmt = connection.createStatement()) {
            // 探测PL对象执行线程是否存活
            stmt.execute(PLUtils.getSpecifiedRoute(this.plDebugODPSpecifiedRoute)
                    + "select dbms_debug.target_program_running() from dual");
            try (ResultSet resultSet = stmt.getResultSet()) {
                if (resultSet.next()) {
                    alive = resultSet.getBoolean(1);
                }
            }
        } catch (Exception e) {
            // lower version of observer may not support this function
            // so just record the message but not throw exception
            log.error("Error occurs while fetching dbms_debug.target_program_running(), debugId={}", debugId, e);
        }
        log.info("Debugger session_id={} alive={}", this.debugId, alive);
        return alive;
    }

    public void continueAbort() {
        executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE, OdcConstants.PROCEDURE_CNT_ABORT, prepareContinueParams());
    }


    public List<PLDebugVariable> getVariables() {
        List<PLDebugVariable> variables = new ArrayList<>();
        List<DBPLParam> params = new ArrayList<>();
        params.add(DBPLParam.of("scalar_values", DBPLParamMode.OUT, "VARCHAR2"));
        params.add(DBPLParam.of("result", DBPLParamMode.OUT, "BINARY_INTEGER"));
        List<DBPLParam> result = executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE,
                OdcConstants.PROCEDURE_GET_VALUES, params);

        int ret = Integer.parseInt(result.get(1).getDefaultValue());
        if (ret != PLDebugErrorCode.success.getId()) {
            PLDebugErrorCode exceptionEnum = PLDebugErrorCode.getEnumById(ret);
            log.error("Error occurs when executing `get_values` operation, debugId={}, errorCode={}, errorMessage={}",
                    debugId, ret,
                    exceptionEnum);
            throw OBException.executePlFailed(
                    String.format("Error occurs when executing `get_values` operation, error_message=%s, ret_code=%s",
                            exceptionEnum.getDebugErrorMessage(ret), ret));

        } else {
            String jsonKV = result.get(0).getDefaultValue();
            if (StringUtils.isNotEmpty(jsonKV)) {
                try {
                    JSONObject jsonObject = JSONObject.parseObject(jsonKV);
                    Iterator<Entry<String, Object>> iterator = jsonObject.entrySet().iterator();
                    while (iterator.hasNext()) {
                        PLDebugVariable odcGetValue = new PLDebugVariable();
                        Entry<String, Object> keyValue = iterator.next();
                        odcGetValue.setName(keyValue.getKey());
                        if (keyValue.getValue() != null) {
                            odcGetValue.setValue(keyValue.getValue().toString());
                        }
                        variables.add(odcGetValue);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse `get_values` result", e);
                    throw OBException.executeFailed(ErrorCodes.DebugInfoParseFailed,
                            "Failed to parse `get_values` result");
                }
            }
        }

        log.debug("Variables fetching finished, size={}", variables.size());
        return variables;
    }

    public PLDebugPrintBacktrace getBacktrace() {
        List<DBPLParam> params = new ArrayList<>();
        params.add(DBPLParam.of("listing", DBPLParamMode.INOUT, "VARCHAR2"));
        params.add(DBPLParam.of("status", DBPLParamMode.OUT, "BINARY_INTEGER"));
        List<DBPLParam> result =
                executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE, OdcConstants.PROCEDURE_PRINT_BACKTRACE, params);

        PLDebugPrintBacktrace printBacktrace = new PLDebugPrintBacktrace();
        String listing = result.get(0).getDefaultValue();
        if (StringUtils.isNotEmpty(listing)) {
            String linenNum = listing.split("Line")[1].split("]")[0].trim();
            String plName = listing.split("]")[1].trim();
            // [Line 0] F_ADD
            // [Line 8] PROC
            if (plName.contains("\n")) {
                plName = plName.split("\n")[0].trim();
            }
            // If a function P1 is defined in a PL object is
            // the stored procedure P1 is called in the execution block
            // and an external stored procedure P1 is exactly defined
            // the bug will be triggered in this case
            if (inLinePlObject(currentDebugPLObject)) {
                plName = currentDebugPLObject.getBasicPlObjectName();
                currentStackInfo.name = currentDebugPLObject.getBasicPlObjectName();
            }
            // in case of function or procedure inside package
            // plName is like PKG.FUNCTION
            // so we need to seperate into packagename and plname
            String[] names = plName.split("\\.");
            if (names.length > 1) {
                printBacktrace.setPackageName(names[0]);
                printBacktrace.setPlName(names[1]);
            } else {
                printBacktrace.setPlName(plName);
            }
            printBacktrace.setLineNum(Integer.parseInt(linenNum));
        }
        printBacktrace.setTerminated(result.get(1).getDefaultValue());
        if (currentStackInfo.stackDepth > 1) {
            // this means we are currently inside some call of function or procedure
            printBacktrace.setDdl(getReferPLObjectDdl(printBacktrace.getLineNum()));
        } else {
            // current stack is 1 or 0
            currentStackInfo.objectType = debugType;
        }
        printBacktrace.setPlType(currentStackInfo.objectType);

        log.debug("Backtrace fetching finished, backtrace={}", printBacktrace);
        return printBacktrace;
    }

    public List<DBPLError> getErrors() {
        DBPLError dbplError = new DBPLError();
        dbplError.setName(objectName);
        dbplError.setType(debugType.name());
        ConnectionConfig connectionConfig =
                (ConnectionConfig) ConnectionSessionUtil.getConnectionConfig(connectionSession);
        return getJdbcOperations()
                .execute(new GetPLErrorCallBack(connectionConfig, dbplError, this.plDebugODPSpecifiedRoute));
    }

    public List<PLDebugBreakpoint> setBreakpoints(List<PLDebugBreakpoint> breakpoints) {
        String schemaName = ConnectionSessionUtil.getCurrentSchema(connectionSession);
        for (PLDebugBreakpoint breakpoint : breakpoints) {
            DBProcedure odcProcedure;
            List<DBPLParam> params = new ArrayList<>();
            if (DBObjectType.FUNCTION == breakpoint.getObjectType()
                    || DBObjectType.PROCEDURE == breakpoint.getObjectType()) {
                // owner param
                DBPLParam ownerParam = DBPLParam.of("owner", DBPLParamMode.IN, "VARCHAR2");
                ownerParam.setDefaultValue(schemaName);
                params.add(ownerParam);
                // name param
                DBPLParam nameParam = DBPLParam.of("name", DBPLParamMode.IN, "VARCHAR2");
                // attention ！！！Observer does not support case like PKG."proc"
                // so we do not need to quote value, just use its original value is okay
                String objectName = breakpoint.getObjectName();
                if (StringUtils.isNotBlank(breakpoint.getPackageName())) {
                    objectName = breakpoint.getPackageName() + "." + objectName;
                }
                nameParam.setDefaultValue(objectName);
                params.add(nameParam);
            }
            DBPLParam lineParam = DBPLParam.of("line#", DBPLParamMode.IN, "BINARY_INTEGER");
            lineParam.setDefaultValue(String.valueOf(breakpoint.getLineNum()));
            params.add(lineParam);
            params.add(DBPLParam.of("breakpoint#", DBPLParamMode.OUT, "BINARY_INTEGER"));
            params.add(DBPLParam.of("result", DBPLParamMode.OUT, "BINARY_INTEGER"));
            if (DBObjectType.FUNCTION == breakpoint.getObjectType()
                    || DBObjectType.PROCEDURE == breakpoint.getObjectType()) {
                odcProcedure =
                        DBProcedure.of(OdcConstants.PL_DEBUG_PACKAGE, OdcConstants.PROCEDURE_SET_BREAKPOINT, params);
            } else if (DBObjectType.ANONYMOUS_BLOCK == breakpoint.getObjectType()) {
                odcProcedure = DBProcedure.of(OdcConstants.PL_DEBUG_PACKAGE,
                        OdcConstants.PROCEDURE_SET_BREAKPOINT_ANONYMOUS, params);
            } else {
                throw OBException.featureNotSupported(
                        String.format("%s is not supported for PLDebug", breakpoint.getObjectType()));
            }

            List<DBPLParam> result = executeDebugProcedure(odcProcedure);
            int ret = Integer.parseInt(result.get(1).getDefaultValue());
            if (ret != PLDebugErrorCode.success.getId()) {
                PLDebugErrorCode exceptionEnum = PLDebugErrorCode.getEnumById(ret);
                log.warn("Error occurs when setting breakpoint={}, error_message={}, ret_code={}", debugId,
                        exceptionEnum.getDebugErrorMessage(ret), ret);
                if (exceptionEnum != PLDebugErrorCode.error_exception) {
                    throw OBException.executePlFailed(
                            String.format("Error occurs when setting breakpoint=%s, error_message=%s, ret_code=%s",
                                    breakpoint, exceptionEnum.getDebugErrorMessage(ret), ret));
                }
            } else {
                breakpoint.setBreakpointNum(Integer.parseInt(result.get(0).getDefaultValue()));
            }
        }
        return breakpoints;
    }

    public Boolean deleteBreakpoints(List<PLDebugBreakpoint> breakpoints) {
        DBFunction function = new DBFunction();
        function.setPackageName("DBMS_DEBUG");
        function.setFunName("DELETE_BREAKPOINT");
        List<DBPLParam> plParamList;
        for (PLDebugBreakpoint breakpoint : breakpoints) {
            plParamList = new ArrayList<>();
            DBPLParam param = DBPLParam.of("breakpoint", DBPLParamMode.IN, "BINARY_INTEGER");
            param.setDefaultValue(String.valueOf(breakpoint.getBreakpointNum()));
            plParamList.add(param);
            function.setParams(plParamList);
            try {
                DBFunction result = executeFunction(function);
                int ret = Integer.parseInt(result.getReturnValue());
                if (ret != PLDebugErrorCode.success.getId()) {
                    PLDebugErrorCode exceptionEnum = PLDebugErrorCode.getEnumById(ret);
                    log.warn("Error occurs when executing continue debug operation, debugId={}, errorCode={}", debugId,
                            exceptionEnum);
                    if (exceptionEnum != PLDebugErrorCode.error_no_such_breakpt) {
                        throw OBException.executePlFailed(
                                String.format("Error occurs when deleting breakpoint=%s, error_message=%s, ret_code=%s",
                                        breakpoint, exceptionEnum.getDebugErrorMessage(ret), ret));
                    }
                }
            } catch (Exception e) {
                log.warn("Error occurs when deleting breakpoint={}", breakpoint, e);
                throw OBException.executePlFailed(String.format("Error occurs when deleting breakpoint=%s, message=%s",
                        breakpoint, e.getMessage()));
            }
        }
        return true;
    }

    public List<PLDebugBreakpoint> listBreakpoints() {
        List<PLDebugBreakpoint> breakpoints = new ArrayList<>();

        List<DBPLParam> params = new ArrayList<>();
        params.add(DBPLParam.of("listing", DBPLParamMode.INOUT, "VARCHAR2"));
        List<DBPLParam> result =
                executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE, OdcConstants.PROCEDURE_SHOW_BREAKPOINTS, params);

        String breakPtsStr = result.get(0).getDefaultValue();
        String[] lines = breakPtsStr.split("\n");
        for (String line : lines) {
            if (line.contains("offset")) {
                // 1. ORACLE_USER.PROC, Line 7 [offset ( 0, 0), 0]
                try {
                    int index = line.indexOf(".");
                    String breakPointNum = line.substring(0, index).trim();
                    String plName = line.substring(index + 1).split(",")[0].trim();
                    String lineNum = line.split("Line")[1].split("\\[offset")[0].trim();
                    PLDebugBreakpoint breakpoint = new PLDebugBreakpoint();
                    breakpoint.setObjectName(plName);
                    breakpoint.setLineNum(Integer.parseInt(lineNum));
                    breakpoint.setBreakpointNum(Integer.parseInt(breakPointNum));

                    breakpoints.add(breakpoint);
                } catch (Exception e) {
                    log.warn("Failed to parse breakpoint info, line={}", line, e);
                }
            }
        }

        return breakpoints;
    }

    public void updateInLinePlObject() {
        if (!inLinePlObject(currentDebugPLObject) && currentDebugPLObject != null) {
            currentDebugPLObject = getCurrentDebugPLObject(currentStackInfo.name, currentStackInfo.owner);
            currentDebugPLObject.setBasicPlObjectType(currentStackInfo.objectType);
            currentDebugPLObject.setBasicPlObjectName(currentStackInfo.name);
        }
    }

    public Boolean inLinePlObject(CurrentDebugPLObject currentDebugPLObject) {
        if (currentDebugPLObject == null) {
            return false;
        }
        if (currentDebugPLObject.getProcedureList() != null) {
            for (DBProcedure odcProcedure : currentDebugPLObject.getProcedureList()) {
                if (Objects.equals(currentStackInfo.name, odcProcedure.getProName())) {
                    return true;
                }
            }
        }
        if (currentDebugPLObject.getFunctionList() != null) {
            for (DBFunction odcFunction : currentDebugPLObject.getFunctionList()) {
                if (Objects.equals(currentStackInfo.name, odcFunction.getFunName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Boolean stepOver() {
        List<DBPLParam> result = executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE,
                OdcConstants.PROCEDURE_CNT_NEXT_LINE, prepareContinueParams());
        trackCurrentStackInfo(result);
        return continueResultHandler(result);
    }

    public Boolean resume() {
        List<DBPLParam> result = executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE,
                OdcConstants.PROCEDURE_CNT_NEXT_BREAKPOINT, prepareContinueParams());
        trackCurrentStackInfo(result);
        return continueResultHandler(result);
    }

    public Boolean stepIn() {
        List<DBPLParam> result = executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE,
                OdcConstants.PROCEDURE_CNT_STEP_IN, prepareContinueParams());
        trackCurrentStackInfo(result);
        updateInLinePlObject();
        return continueResultHandler(result);
    }

    public Boolean stepInForStartingDebug() {
        int tryTimes = 0;
        int depth = currentStackInfo.stackDepth;
        List<DBPLParam> result = new ArrayList<>();
        while (currentStackInfo.stackDepth <= depth && tryTimes < MAX_TRY_STEP_INTO_TIMES) {
            tryTimes++;
            result = executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE, OdcConstants.PROCEDURE_CNT_STEP_IN,
                    prepareContinueParams());
            trackCurrentStackInfo(result);
        }
        if (currentStackInfo.stackDepth <= depth) {
            throw OBException.executeFailed(ErrorCodes.DebugStartFailed,
                    String.format("Failed to step-in with debuggee, stack info: %s", currentStackInfo));
        }
        updateInLinePlObject();
        return continueResultHandler(result);
    }

    public Boolean stepOut() {
        List<DBPLParam> result = executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE,
                OdcConstants.PROCEDURE_CNT_STEP_OUT, prepareContinueParams());
        trackCurrentStackInfo(result);
        return continueResultHandler(result);
    }

    public Boolean resumeIgnoreBreakpoints() {
        List<DBPLParam> result = executeDebugProcedure(OdcConstants.PL_DEBUG_PACKAGE,
                OdcConstants.PROCEDURE_CNT_EXIT, prepareContinueParams());
        return continueResultHandler(result);
    }

    private List<DBPLParam> executeDebugProcedure(String packageName, String proName,
            List<DBPLParam> params) {
        DBProcedure odcProcedure = DBProcedure.of(packageName, proName, params);
        return executeDebugProcedure(odcProcedure);
    }

    private List<DBPLParam> executeDebugProcedure(DBProcedure dbProcedure) {
        return executeProcedure(dbProcedure);
    }

    private List<DBPLParam> prepareContinueParams() {
        List<DBPLParam> plParamList = new ArrayList<>();
        DBPLParam param = new DBPLParam();
        param.setParamName("result");
        param.setParamMode(DBPLParamMode.OUT);
        param.setDataType("BINARY_INTEGER");
        plParamList.add(param);

        param = new DBPLParam();
        param.setParamName("message");
        param.setParamMode(DBPLParamMode.OUT);
        param.setDataType("VARCHAR2");
        plParamList.add(param);
        return plParamList;
    }

    private Boolean continueResultHandler(List<DBPLParam> result) {
        int ret = Integer.parseInt(result.get(0).getDefaultValue());
        if (ret != PLDebugErrorCode.success.getId()) {
            PLDebugErrorCode exceptionEnum = PLDebugErrorCode.getEnumById(ret);
            log.warn("Error occurs when executing continue debug operation, debugId={}, errorCode={}, retCode={}",
                    debugId, exceptionEnum, ret);
            throw OBException.executePlFailed(String.format(
                    "Error occurs while executing debug operation, debugId=%s, error_message=%s, ret_code=%s",
                    debugId, exceptionEnum.getDebugErrorMessage(ret), ret));
        }
        return true;
    }

    static class StackInfo {
        int stackDepth;
        String owner;
        String name;
        PLDebugStatusReason reason;
        DBObjectType objectType;

        StackInfo() {
            stackDepth = 1;
        }
    }

    private void trackCurrentStackInfo(List<DBPLParam> result) {
        for (DBPLParam param : result) {
            if ("message".equals(param.getParamName())) {
                // message example: run_info.breakpoint = , run_info.stackdepth = 2, run_info.reason = 9,
                // run_info.programname = PRO1, run_info.programowner = CHZ
                String message = param.getDefaultValue();
                log.info("Current stack message:{}", message);

                String[] runInfos = message.split(",");
                Validate.isTrue(runInfos.length >= 5,
                        String.format("runInfo message={%s} length should be >= 5", message));
                // stack depth can not be null，if it is null, throw an exception
                String[] stackdepthInfos = runInfos[1].split(" = ");
                if (stackdepthInfos.length < 2) {
                    throw new BadRequestException(ErrorCodes.PLDebugKernelUnknownError,
                            new Object[] {"stackDepth is null"}, message);
                }
                currentStackInfo.stackDepth = Integer.parseInt(stackdepthInfos[1]);
                log.info("current stack info stackdepth:{}", currentStackInfo.stackDepth);

                // record current status after this operation
                String[] reasonInfo = runInfos[2].split(" = ");
                if (reasonInfo.length >= 2) {
                    int reasonNumber;
                    try {
                        reasonNumber = Integer.parseInt(reasonInfo[1]);
                    } catch (Exception e) {
                        reasonNumber = 0;
                    }
                    currentStackInfo.reason = PLDebugStatusReason.getEnumById(reasonNumber);
                    log.info("current stack info reason:{}", currentStackInfo.reason);
                }
                // name or owner maybe null when pl running comes to end
                String[] nameInfo = runInfos[3].split(" = ");
                if (nameInfo.length >= 2) {
                    currentStackInfo.name = nameInfo[1];
                }
                String[] ownerInfo = runInfos[4].split(" = ");
                if (ownerInfo.length >= 2) {
                    currentStackInfo.owner = ownerInfo[1];
                }
            }
        }
    }

    private String getReferPLObjectDdl(Integer lineNum) {
        String referDdl = null;
        try {
            String objectName = currentStackInfo.name;
            if (objectName.contains(".")) {
                // 如果包含 . 说明此时是程序包，我们需要解析得到当前程序包包体下目标对象的类型
                String packageName = objectName.split("\\.")[0];
                objectName = objectName.split("\\.")[1];
                DBPackageDetail packageDetail = this.debugPackageMap.computeIfAbsent(
                        new PackageKey(currentStackInfo.owner, packageName),
                        k -> getPackageBodyObject(k.getPackageName(), k.getOwner()));
                currentStackInfo.objectType = getDbObjectType(packageDetail, objectName, lineNum);
                return null;
            }
            Object fetchResult =
                    DBPLOperators.create(connectionSession)
                            .getPLObject(PLIdentity.of(null, objectName, currentStackInfo.owner));
            if (fetchResult instanceof DBProcedure) {
                currentStackInfo.objectType = DBObjectType.PROCEDURE;
                referDdl = ((DBProcedure) fetchResult).getDdl();
            } else if (fetchResult instanceof DBFunction) {
                currentStackInfo.objectType = DBObjectType.FUNCTION;
                referDdl = ((DBFunction) fetchResult).getDdl();
            } else if (fetchResult instanceof DBPackage) {
                DBPackage odcPackage = (DBPackage) fetchResult;
                if (DBObjectType.PACKAGE.getName().equalsIgnoreCase(odcPackage.getPackageType())) {
                    currentStackInfo.objectType = DBObjectType.PACKAGE;
                    referDdl = odcPackage.getPackageHead().getBasicInfo().getDdl();
                } else {
                    currentStackInfo.objectType = DBObjectType.PACKAGE_BODY;
                    referDdl = odcPackage.getPackageBody().getBasicInfo().getDdl();
                }
            } else if (fetchResult instanceof DBTrigger) {
                currentStackInfo.objectType = DBObjectType.TRIGGER;
                referDdl = ((DBTrigger) fetchResult).getDdl();
            } else if (fetchResult instanceof DBType) {
                currentStackInfo.objectType = DBObjectType.TYPE;
                referDdl = ((DBType) fetchResult).getDdl();
            }
        } catch (Exception e) {
            log.warn("Error occurs while fetching refer object ddl, debugId={}, objectName={}", debugId,
                    currentStackInfo.name, e);
            throw OBException.executeFailed(ErrorCodes.PLObjectFetchFailed, e.getMessage());
        }

        return referDdl;
    }

    private DBObjectType getDbObjectType(@NonNull DBBasicPLObject plObject,
            @NonNull String objectName, Integer lineNum) {
        if (lineNum != null) {
            Optional<DBProcedure> optional = plObject.getProcedures().stream()
                    .filter(p -> p.getStartline() <= lineNum && p.getStopLine() >= lineNum).findAny();
            if (optional.isPresent()) {
                return DBObjectType.PROCEDURE;
            }
            Optional<DBFunction> optional1 = plObject.getFunctions().stream()
                    .filter(f -> f.getStartline() <= lineNum && f.getStopLine() >= lineNum).findAny();
            if (optional1.isPresent()) {
                return DBObjectType.FUNCTION;
            }
        }
        Optional<DBProcedure> optional = plObject.getProcedures().stream()
                .filter(p -> StringUtils.equalsIgnoreCase(p.getProName(), objectName)).findAny();
        if (optional.isPresent()) {
            return DBObjectType.PROCEDURE;
        }
        Optional<DBFunction> optional1 = plObject.getFunctions().stream()
                .filter(f -> StringUtils.equalsIgnoreCase(f.getFunName(), objectName)).findAny();
        if (optional1.isPresent()) {
            return DBObjectType.FUNCTION;
        }
        throw new IllegalStateException("Can not find db type by object name, " + objectName);
    }

    private DBPackageDetail getPackageBodyObject(String packageName, String owner) {
        Object fetchResult;
        try {
            fetchResult =
                    DBPLOperators.create(connectionSession)
                            .getPLObject(PLIdentity.of(DBObjectType.PACKAGE_BODY, packageName, owner));

        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        DBPackage odcPackage = (DBPackage) fetchResult;
        String ddl = odcPackage.getPackageBody().getBasicInfo().getDdl();
        Verify.notBlank(ddl, "PackageBodyDdl");
        ddl = StringUtils.startsWithIgnoreCase(ddl, "create") ? ddl : "create or replace " + ddl;
        DBPackageDetail packageBody = new DBPackageDetail();

        ParseOraclePLResult result = PLParser.parseOracle(ddl);
        packageBody.setVariables(result.getVaribaleList());
        packageBody.setTypes(result.getTypeList());
        packageBody.setFunctions(result.getFunctionList());
        packageBody.setProcedures(result.getProcedureList());
        return packageBody;
    }

    @Getter
    @Setter
    @EqualsAndHashCode
    static class PackageKey {
        private final String owner;
        private final String packageName;

        public PackageKey(@NonNull String owner, @NonNull String packageName) {
            this.owner = owner;
            this.packageName = packageName;
        }
    }

    private CurrentDebugPLObject getCurrentDebugPLObject(String objectName, String owner) {
        if (objectName.contains(".")) {
            // 如果包含 . 说明此时是程序包，我们需要解析得到当前程序包包体下目标对象的类型
            DBPackageDetail packageDetail = this.debugPackageMap.computeIfAbsent(
                    new PackageKey(currentStackInfo.owner, objectName.split("\\.")[0]),
                    k -> getPackageBodyObject(k.getPackageName(), k.getOwner()));
            currentStackInfo.objectType = getDbObjectType(packageDetail, objectName.split("\\.")[1], null);
            CurrentDebugPLObject currentDebugPLObject = new CurrentDebugPLObject(new OracleModeParserListener());
            currentDebugPLObject.setFunctionList(packageDetail.getFunctions());
            currentDebugPLObject.setProcedureList(packageDetail.getProcedures());
            return currentDebugPLObject;
        }
        Object fetchResult;
        try {
            fetchResult = DBPLOperators.create(connectionSession)
                    .getPLObject(PLIdentity.of(null, objectName, owner));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        String ddl = null;
        if (fetchResult instanceof DBProcedure) {
            currentStackInfo.objectType = DBObjectType.PROCEDURE;
            ddl = ((DBProcedure) fetchResult).getDdl();
        } else if (fetchResult instanceof DBFunction) {
            currentStackInfo.objectType = DBObjectType.FUNCTION;
            ddl = ((DBFunction) fetchResult).getDdl();
        }
        return parserDdl(ddl);
    }

    private CurrentDebugPLObject parserDdl(String ddl) {
        Verify.notBlank(ddl, "PLObjectDdl");
        CurrentDebugPLObject currentDebugPLObject = new CurrentDebugPLObject(new OracleModeParserListener());
        ParseOraclePLResult result = PLParser.parseOracle(ddl);
        if (result.getFunctionList() != null) {
            currentDebugPLObject.setFunctionList(result.getFunctionList());
        }
        if (result.getProcedureList() != null) {
            currentDebugPLObject.setProcedureList(result.getProcedureList());
        }
        return currentDebugPLObject;
    }

}
