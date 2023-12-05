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
package com.oceanbase.odc.service.pldebug;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.oceanbase.odc.common.concurrent.ExecutorUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.OBException;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.pldebug.model.PLDebugBreakpoint;
import com.oceanbase.odc.service.pldebug.model.PLDebugConstants;
import com.oceanbase.odc.service.pldebug.model.PLDebugContextResp;
import com.oceanbase.odc.service.pldebug.model.PLDebugVariable;
import com.oceanbase.odc.service.pldebug.model.StartPLDebugReq;
import com.oceanbase.odc.service.pldebug.operator.DBPLOperators;
import com.oceanbase.odc.service.pldebug.session.PLDebugSession;
import com.oceanbase.odc.service.regulation.ruleset.SqlConsoleRuleService;
import com.oceanbase.odc.service.regulation.ruleset.model.SqlConsoleRules;
import com.oceanbase.odc.service.session.SessionProperties;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author wenniu.ly
 * @date 2021/11/12
 */

@Slf4j
@Service
@SkipAuthorize("inside connect session")
public class PLDebugService {

    private final Map<String, PLDebugSession> debugId2Session = new ConcurrentHashMap<>();

    @Value("${odc.pldebug.thread-pool.size:0}")
    private int maxPoolSize;
    @Value("${odc.pldebug.session.timeout-seconds:600}")
    private int sessionTimeoutSeconds;
    @Value("${odc.pldebug.sync.enabled:true}")
    private boolean syncEnabled;
    @Autowired
    private SessionProperties sessionProperties;
    private ThreadPoolExecutor debugSessionExecutor;
    private ScheduledExecutorService debugMonitorExecutor;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private SqlConsoleRuleService sqlConsoleRuleService;

    @PostConstruct
    public void init() {
        if (maxPoolSize <= 0) {
            maxPoolSize = 4 * SystemUtils.availableProcessors();
        }
        // thread control in thread pool
        // core pool size equals to max is for submitting new thread without blocking
        this.debugSessionExecutor = new ThreadPoolExecutor(maxPoolSize, maxPoolSize, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1),
                r -> new Thread(r, "pldebug-session-" + r.hashCode()),
                new ThreadPoolExecutor.CallerRunsPolicy());

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("pldebug-monitor-%d")
                .build();
        debugMonitorExecutor = new ScheduledThreadPoolExecutor(1, threadFactory);
        debugMonitorExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                log.info("Number of running pl debug session={}", debugId2Session.size());
                Iterator<Entry<String, PLDebugSession>> iterator = debugId2Session.entrySet().iterator();
                while (iterator.hasNext()) {
                    PLDebugSession plDebugSession = iterator.next().getValue();
                    if (plDebugSession.isTimeOut()) {
                        log.info("PLDebug session id={} is time out, last access time: {}", plDebugSession.getDebugId(),
                                plDebugSession.getLastAccessTime());
                        try {
                            plDebugSession.end();
                            iterator.remove();
                        } catch (Exception e) {
                            log.error("Error occurs while closing pldebug session={}", plDebugSession.getDebugId(), e);
                        }
                    }
                }
            }
        }, 5, 10, TimeUnit.MINUTES);
        log.info("PLDebug Service initialize finished");
    }

    @PreDestroy
    public void destroy() {
        log.info("PLDebug Service start to destroy...");
        ExecutorUtils.gracefulShutdown(debugSessionExecutor, "debugSessionExecutor", 5);
        ExecutorUtils.gracefulShutdown(debugMonitorExecutor, "debugMonitorExecutor", 5);
        log.info("PLDebug Service destroyed");
    }

    public String start(@NonNull ConnectionSession connectionSession, StartPLDebugReq req) throws Exception {
        if (sqlConsoleRuleService.isForbidden(SqlConsoleRules.NOT_ALLOWED_DEBUG_PL, connectionSession)) {
            throw new BadRequestException("not allow to debug pl due to the sql console rule settings");
        }
        validate(req);
        // test whether DBMS_DEBUG package exist
        Boolean isSupported = DBPLOperators.create(connectionSession).isSupportPLDebug();
        if (!isSupported) {
            log.error("DBMS_DEBUG not supported");
            throw OBException.executeFailed(ErrorCodes.DebugDBMSNotSupported, "DBMS_DEBUG not supported");
        }
        // create odc debug package if not exists or valid
        try {
            DBPLOperators.create(connectionSession).createDebugPLPackage();
            log.info("Created OBODC_PL_DEBUG_PACKAGE package successfully");
        } catch (Exception e) {
            log.error(
                    "Fail to create OBODC_PL_DEBUG_PACKAGE package when connecting session, packageName={}, schemaName={}",
                    PLDebugConstants.PL_DEBUG_PACKAGE, ConnectionSessionUtil.getCurrentSchema(connectionSession), e);
            throw OBException.executeFailed(ErrorCodes.DebugPackageCreateFailed, e.getMessage());
        }
        PLDebugSession plDebugSession = new PLDebugSession(authenticationFacade.currentUserId());
        plDebugSession.setDbmsoutputMaxRows(sessionProperties.getDbmsOutputMaxRows());
        plDebugSession.start(connectionSession, debugSessionExecutor, req, sessionTimeoutSeconds, syncEnabled);
        debugId2Session.put(plDebugSession.getSessionId(), plDebugSession);
        return plDebugSession.getSessionId();
    }

    private void validate(StartPLDebugReq req) {
        Validate.notNull(req.getDebugType(), "DebugType can not be null for StartPLDebugReq");
        if (DBObjectType.ANONYMOUS_BLOCK == req.getDebugType()) {
            Validate.notEmpty(req.getAnonymousBlock(), "AnonymousBlock can not be null for StartPLDebugReq");
        } else if (DBObjectType.FUNCTION == req.getDebugType()) {
            Validate.notNull(req.getFunction(), "Function can not be null for StartPLDebugReq");
        } else if (DBObjectType.PROCEDURE == req.getDebugType()) {
            Validate.notNull(req.getProcedure(), "Procedure can not be null for StartPLDebugReq");
        } else {
            throw OBException.featureNotSupported(String.format("%s is not supported for PLDebug", req.getDebugType()));
        }
    }

    public String end(String debugId) {
        PLDebugSession session = nullSafeGet(debugId);
        try {
            session.end();
        } catch (Exception e) {
            log.error("Cannot close PLDebug connection, debugId={}, reason: ", debugId, e);
        }
        debugId2Session.remove(debugId);
        return debugId;
    }

    public PLDebugContextResp getContext(String debugId) {
        return nullSafeGet(debugId).getContext();
    }

    public List<PLDebugBreakpoint> setBreakpoints(String debugId, List<PLDebugBreakpoint> breakpoints) {
        return nullSafeGet(debugId).setBreakpoints(breakpoints);
    }

    public Boolean deleteBreakpoints(String debugId, List<PLDebugBreakpoint> breakpoints) {
        return nullSafeGet(debugId).deleteBreakpoints(breakpoints);
    }

    public List<PLDebugBreakpoint> listBreakpoints(String debugId) {
        return nullSafeGet(debugId).listBreakpoints();
    }

    public List<PLDebugVariable> getVariables(String debugId) {
        return nullSafeGet(debugId).getVariables();
    }

    public Boolean stepOver(String debugId) {
        return nullSafeGet(debugId).stepOver();
    }

    public Boolean resume(String debugId) {
        return nullSafeGet(debugId).resume();
    }

    public Boolean stepIn(String debugId) {
        return nullSafeGet(debugId).stepIn();
    }

    public Boolean stepOut(String debugId) {
        return nullSafeGet(debugId).stepOut();
    }

    public Boolean resumeIgnoreBreakpoints(String debugId) {
        return nullSafeGet(debugId).resumeIgnoreBreakpoints();
    }

    private PLDebugSession nullSafeGet(String debugSessionId) {
        PLDebugSession session = debugId2Session.get(debugSessionId);
        if (Objects.isNull(session)) {
            // TODO: here my not found or timeout
            throw OBException.executeFailed(ErrorCodes.DebugTimeout,
                    String.format("Debug timeout for %ds", sessionTimeoutSeconds));
        }
        PreConditions.validExists(ResourceType.ODC_PL_DEBUG_SESSION, "debugSessionId", debugSessionId,
                () -> session.getUserId() == authenticationFacade.currentUserId());

        session.touch();
        return session;
    }

}
