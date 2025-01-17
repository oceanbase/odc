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

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.service.db.model.DBMSOutput;
import com.oceanbase.odc.service.pldebug.model.DBPLError;
import com.oceanbase.odc.service.pldebug.model.PLDebugBreakpoint;
import com.oceanbase.odc.service.pldebug.model.PLDebugContextResp;
import com.oceanbase.odc.service.pldebug.model.PLDebugPrintBacktrace;
import com.oceanbase.odc.service.pldebug.model.PLDebugResult;
import com.oceanbase.odc.service.pldebug.model.PLDebugVariable;
import com.oceanbase.odc.service.pldebug.model.StartPLDebugReq;
import com.oceanbase.odc.service.pldebug.util.PLUtils;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/11/18
 */

@Data
@Slf4j
public class PLDebugSession {
    /**
     * ODC 维护的 PL 调试会话 ID，注意和 OBServer 的 debugId 概念不同，这里是维护 ODC 全局唯一的 debugSessionId
     */
    private final String sessionId;
    /**
     * 发起调试的用户
     */
    private final long userId;
    private volatile boolean debugOn;
    private DebuggeeSession debuggeeSession;
    private DebuggerSession debuggerSession;
    private ScheduledExecutorService scheduleExecutor;
    private Long lastAccessTime;
    private Long timeoutMilliSeconds;
    @Setter
    private Integer dbmsoutputMaxRows = null;

    public PLDebugSession(long userId, IdGenerator idGenerator) {
        this.sessionId = idGenerator.generate();
        this.userId = userId;
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("PLDebug-schedule-ping-%d")
                .build();
        scheduleExecutor = new ScheduledThreadPoolExecutor(1, threadFactory);
        scheduleExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                if (Objects.nonNull(debuggerSession) && debuggerSession.detectSessionAlive()) {
                    try (Statement stmt = debuggerSession.getConnection().createStatement()) {
                        stmt.execute(PLUtils.getSpecifiedRoute(debuggerSession.getPlDebugODPSpecifiedRoute())
                                + "CALL DBMS_DEBUG.PING()");
                    } catch (Exception e) {
                        log.debug("Failed to call DBMS_DEBUG.PING()", e);
                    }
                }
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    public void touch() {
        lastAccessTime = System.currentTimeMillis();
    }

    public boolean isTimeOut() {
        return System.currentTimeMillis() - lastAccessTime > timeoutMilliSeconds;
    }

    public void start(ConnectionSession connectionSession, ThreadPoolExecutor debugThreadPoolExecutor,
            StartPLDebugReq req, long timeoutSeconds, boolean syncEnabled) throws Exception {
        try {
            debuggeeSession = new DebuggeeSession(connectionSession, debugThreadPoolExecutor, req);
            debuggeeSession.setDbmsoutputMaxRows(dbmsoutputMaxRows);
            debuggerSession = new DebuggerSession(debuggeeSession, req, syncEnabled);
            if (!debuggerSession.detectSessionAlive() && !debuggeeSession.detectSessionAlive()) {
                PLDebugResult debugResult = debuggeeSession.getResult();
                throw OBException.executeFailed(ErrorCodes.DebugStartFailed,
                        String.format("Debug start failed, reason=%s", debugResult.getExecutionErrorMessage()));
            }
        } catch (Exception e) {
            end();
            throw e;
        }
        debugOn = true;
        lastAccessTime = System.currentTimeMillis();
        this.timeoutMilliSeconds = timeoutSeconds * 1000;
    }

    public synchronized void end() throws Exception {
        closeDebugger();
        closeDebuggee();
        ExecutorUtils.gracefulShutdown(scheduleExecutor, "debugPingScheduleExecutor", 5);
    }

    public synchronized PLDebugContextResp getContext() {
        PLDebugContextResp plDebugContextResp = new PLDebugContextResp();
        if (debuggerSession == null || debuggeeSession == null) {
            throw OBException.executeFailed(ErrorCodes.DebugTimeout,
                    String.format("Debug timeout for %d ms", timeoutMilliSeconds));
        }
        // terminated
        boolean terminated = !debuggerSession.detectSessionAlive() || !debuggeeSession.detectSessionAlive();
        plDebugContextResp.setTerminated(terminated);
        if (terminated) {
            // detach session & debug_off
            detach();
            debugOff();
        }

        List<DBPLError> errors = new ArrayList<>();
        if (debugOn) {
            // variables
            try {
                List<PLDebugVariable> variables = debuggerSession.getVariables();
                plDebugContextResp.setVariables(variables);
            } catch (Exception e) {
                log.debug("Failed to parse `get_values` result when getting context");
            }
            // backtrace
            PLDebugPrintBacktrace backtrace = debuggerSession.getBacktrace();
            plDebugContextResp.setBacktrace(backtrace);
        } else {
            // dbms output
            DBMSOutput dbmsOutput = debuggeeSession.getOutput();
            plDebugContextResp.setDbmsOutput(dbmsOutput);
            // result
            PLDebugResult result = debuggeeSession.getResult();
            if (Objects.nonNull(result)) {
                if (Objects.nonNull(result.getFunctionResult())) {
                    plDebugContextResp.setFunctionResult(result.getFunctionResult());
                } else if (Objects.nonNull(result.getProcedureResult())) {
                    plDebugContextResp.setProcedureResult(result.getProcedureResult());
                } else {
                    // to acquire running errors
                    DBPLError error = new DBPLError();
                    if (result.getExecutionErrorMessage() != null) {
                        error.setText(result.getExecutionErrorMessage());
                        errors.add(error);
                    }
                }
            }
        }
        // errors
        errors.addAll(debuggerSession.getErrors());
        plDebugContextResp.setErrors(errors);
        return plDebugContextResp;
    }

    public synchronized List<PLDebugBreakpoint> setBreakpoints(List<PLDebugBreakpoint> breakpoints) {
        return debuggerSession.setBreakpoints(breakpoints);
    }

    public synchronized Boolean deleteBreakpoints(List<PLDebugBreakpoint> breakpoints) {
        return debuggerSession.deleteBreakpoints(breakpoints);
    }

    public synchronized List<PLDebugBreakpoint> listBreakpoints() {
        return debuggerSession.listBreakpoints();
    }

    public synchronized List<PLDebugVariable> getVariables() {
        return debuggerSession.getVariables();
    }

    public synchronized Boolean stepOver() {
        return debuggerSession.stepOver();
    }

    public synchronized Boolean resume() {
        return debuggerSession.resume();
    }

    public synchronized Boolean stepIn() {
        return debuggerSession.stepIn();
    }

    public synchronized Boolean stepOut() {
        return debuggerSession.stepOut();
    }

    public synchronized Boolean resumeIgnoreBreakpoints() {
        return debuggerSession.resumeIgnoreBreakpoints();
    }

    public String getDebugId() {
        return debuggeeSession.getDebugId();
    }

    private void closeDebugger() throws Exception {
        if (Objects.isNull(debuggerSession)) {
            return;
        }
        // execute cnt abort procedure
        debuggerSession.continueAbort();
        detach();
        debuggerSession.close();
        debuggerSession = null;
    }

    private void closeDebuggee() throws Exception {
        if (Objects.isNull(debuggeeSession)) {
            return;
        }
        if (!debugOn) {
            return;
        }
        debugOff();
        debuggeeSession.close();
        debuggeeSession = null;
    }

    private void detach() {
        try (Statement stmt = debuggerSession.getConnection().createStatement()) {
            stmt.execute(PLUtils.getSpecifiedRoute(debuggerSession.getPlDebugODPSpecifiedRoute())
                    + "call dbms_debug.detach_session();");
        } catch (Exception e) {
            log.warn("fail to detach session on debugger", e);
        }
    }

    private void debugOff() {
        try (Statement stmt = debuggeeSession.getConnection().createStatement()) {
            stmt.execute(PLUtils.getSpecifiedRoute(debuggeeSession.getPlDebugODPSpecifiedRoute())
                    + "call dbms_debug.debug_off();");
        } catch (Exception e) {
            log.warn("fail to debug off on debuggee", e);
        }
        // ensure debugOn will be false
        debugOn = false;
    }

    public interface IdGenerator {
        String generate();
    }
}
