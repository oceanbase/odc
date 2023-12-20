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
package com.oceanbase.odc.service.audit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.util.Strings;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventResult;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.sql.execute.model.SqlExecuteStatus;
import com.oceanbase.odc.metadb.audit.AuditEventEntity;
import com.oceanbase.odc.metadb.connection.DatabaseEntity;
import com.oceanbase.odc.metadb.connection.DatabaseRepository;
import com.oceanbase.odc.service.audit.model.AuditEvent;
import com.oceanbase.odc.service.audit.model.AuditEventMeta;
import com.oceanbase.odc.service.audit.util.AuditEventMapper;
import com.oceanbase.odc.service.audit.util.AuditUtils;
import com.oceanbase.odc.service.common.model.SetEnabledReq;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.session.model.SqlTuplesWithViolation;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/1/17 下午5:54
 * @Description: []
 */
@Component
@Slf4j
@Aspect
@ConditionalOnProperty(name = "odc.audit.enabled", havingValue = "true")
public class AuditEventAspect {
    private final String COMMA = ",";

    private final int MAX_DETAIL_LENGTH = 20000;

    private final LocalVariableTableParameterNameDiscoverer nameDiscoverer =
            new LocalVariableTableParameterNameDiscoverer();

    private final ExpressionParser parser = new SpelExpressionParser();

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditEventMetaService auditEventMetaService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private HttpServletRequest servletRequest;

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ConnectSessionService sessionService;

    @Autowired
    private AuditEventHandler auditEventHandler;

    private AuditEventMapper mapper = AuditEventMapper.INSTANCE;

    @Pointcut("execution(public * com.oceanbase.odc.server.web.controller.*.*.*(..))")
    public void eventAudit() {}

    @Pointcut("execution(public * com.oceanbase.odc.server.web.controller.v2.ConnectSessionController.getAsyncSqlExecute(..))")
    public void getAsyncSqlExecuteResult() {}

    @Around("eventAudit()")
    public Object aroundEventAudit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] objects = joinPoint.getArgs();
        AuditEvent auditEvent;
        try {
            auditEvent = createAuditEvent(method, objects);
        } catch (Exception ex) {
            log.warn("create audit event failed, cause={}", ex.getCause());
            return joinPoint.proceed();
        }

        if (Objects.isNull(auditEvent)) {
            return joinPoint.proceed();
        }
        try {
            Object processResult = joinPoint.proceed();
            saveAuditEvents(auditEvent, processResult);
            return processResult;
        } catch (Exception ex) {
            saveAuditEventsWithResult(auditEvent, AuditEventResult.FAILED);
            throw ex;
        }
    }

    @AfterReturning(value = "getAsyncSqlExecuteResult()", returning = "returnValue")
    public void afterSqlExecute(Object returnValue) {
        Object data = null;
        if (returnValue instanceof SuccessResponse) {
            data = ((SuccessResponse) returnValue).getData();
        }
        List<AuditEventEntity> events = new ArrayList<>();
        for (Object obj : (List) data) {
            SqlExecuteResult result = (SqlExecuteResult) obj;
            AuditEventResult auditEventResult = getAuditEventResultFromResult(result);
            if (AuditEventResult.UNFINISHED != auditEventResult) {
                AuditEventAction action = AuditUtils.getSqlTypeFromResult(result.getSqlType());
                String detail = truncateDetail(getDetailFromResult(result));
                String taskId = result.getSqlId();
                // The sql id of resultSet has suffix "-" + increment number, @See SqlTuple#softCopy
                int last = result.getSqlId().lastIndexOf("-");
                if (last > 0) {
                    taskId = result.getSqlId().substring(0, last);
                }
                AuditEventEntity eventEntity =
                        auditEventService.updateSqlExecuteEvent(taskId, action, detail, auditEventResult);
                if (eventEntity != null) {
                    events.add(eventEntity);
                }
            }
        }
        this.auditEventHandler.handle(events, servletRequest);
    }

    private AuditEventResult getAuditEventResultFromResult(SqlExecuteResult result) {
        if (SqlExecuteStatus.SUCCESS == result.getStatus()) {
            return AuditEventResult.SUCCESS;
        } else if (SqlExecuteStatus.RUNNING == result.getStatus()) {
            return AuditEventResult.UNFINISHED;
        } else {
            return AuditEventResult.FAILED;
        }
    }

    private String getDetailFromResult(SqlExecuteResult result) {
        if (Objects.nonNull(result)) {
            return result.getExecuteSql();
        }
        return Strings.EMPTY;
    }


    private void saveAuditEvents(AuditEvent auditEvent, Object processResult) {
        List<String> taskIds = parseTaskIds(auditEvent, processResult);
        if (CollectionUtils.isNotEmpty(taskIds)) {
            auditEventService.saveAsyncTaskEvent(auditEvent, taskIds);
            this.auditEventHandler.handle(Collections.singletonList(mapper.modelToEntity(auditEvent)), servletRequest);
            return;
        }
        saveAuditEventsWithResult(auditEvent, AuditEventResult.SUCCESS);
    }

    private List<String> parseTaskIds(AuditEvent auditEvent, Object processResult) {
        auditEvent.setResult(AuditEventResult.SUCCESS);
        Object data = null;
        if (processResult instanceof SuccessResponse) {
            data = ((SuccessResponse) processResult).getData();
        }
        if (Objects.isNull(data)) {
            return Collections.emptyList();
        }

        /**
         * 流程任务相关的方法，需要从出参中获取具体的任务类型
         */
        if (AuditEventAction.EXECUTE_TASK == auditEvent.getAction()
                || AuditEventAction.APPROVE == auditEvent.getAction()
                || AuditEventAction.REJECT == auditEvent.getAction()
                || AuditEventAction.STOP_TASK == auditEvent.getAction()) {
            auditEvent.setType(AuditUtils.getEventTypeFromTaskType(((FlowInstanceDetailResp) data).getType()));
            auditEvent.setAction(AuditUtils.getActualActionForTask(auditEvent.getType(), auditEvent.getAction()));
        }
        /**
         * Sql 异步执行
         */
        if (data instanceof SqlAsyncExecuteResp) {
            auditEvent.setResult(AuditEventResult.UNFINISHED);
            List<SqlTuplesWithViolation> sqlTuplesWithViolations = ((SqlAsyncExecuteResp) data).getSqls();
            return sqlTuplesWithViolations.stream()
                    .map(tuple -> tuple.getSqlTuple().getSqlId())
                    .collect(Collectors.toList());
        }
        if (data instanceof FlowInstanceDetailResp) {
            FlowInstanceDetailResp flowInstanceDetailResp = (FlowInstanceDetailResp) data;
            Long taskId = flowInstanceDetailResp.getId();
            return Arrays.asList(String.valueOf(taskId));
        }

        return Collections.emptyList();
    }

    private void saveAuditEventsWithResult(AuditEvent auditEvent, AuditEventResult result) {
        auditEvent.setResult(result);
        auditEvent.setEndTime(new Date());
        auditEventService.record(auditEvent);
        this.auditEventHandler.handle(Collections.singletonList(mapper.modelToEntity(auditEvent)), servletRequest);
    }

    private AuditEvent createAuditEvent(Method method, Object[] args) {
        Optional<AuditEventMeta> optional =
                auditEventMetaService.findAuditEventMetaByMethodSignatureIfEnabled(getMethodSignature(method));
        if (!optional.isPresent()) {
            return null;
        }
        Map<String, Object> apiParams = getApiParams(method, args);
        AuditEventMeta auditEventMeta = optional.get();
        AuditEvent auditEvent = AuditEvent.builder()
                .action(parseActualAction(auditEventMeta.getAction(), apiParams))
                .type(auditEventMeta.getType())
                .startTime(new Date())
                .serverIpAddress(SystemUtils.getLocalIpAddress())
                .clientIpAddress(WebRequestUtils.getClientAddress(servletRequest))
                .organizationId(authenticationFacade.currentOrganizationId())
                .userId(authenticationFacade.currentUserId())
                .username(authenticationFacade.currentUsername())
                .detail(parseDetailFromApiParams(apiParams))
                .build();

        if (StringUtils.isNotBlank(auditEventMeta.getDatabaseIdExtractExpression())) {
            Long databaseId = parseDatabaseId(auditEventMeta, method, args);
            if (Objects.nonNull(databaseId)) {
                DatabaseEntity database =
                        databaseRepository.findById(databaseId).orElseThrow(() -> new NotFoundException(
                                ResourceType.ODC_DATABASE, "id", databaseId));
                auditEvent.setDatabaseId(databaseId);
                auditEvent.setDatabaseName(database.getName());
                setConnectionRelatedProperties(auditEvent, String.valueOf(database.getConnectionId()));
            }
        }

        if (StringUtils.isNotBlank(auditEventMeta.getSidExtractExpression())) {
            String sid = parseSid(auditEventMeta, method, args);
            if (StringUtils.isNotEmpty(sid)) {
                auditEvent = setConnectionRelatedProperties(auditEvent, sid);
            }
        }

        // 如果是创建任务流程，需要从请求中获取具体的 type
        if (Objects.nonNull(auditEvent) && AuditEventAction.CREATE_TASK == auditEventMeta.getAction()) {
            CreateFlowInstanceReq req = (CreateFlowInstanceReq) apiParams.get("flowInstanceReq");
            if (Objects.nonNull(req)) {
                AuditEventType type = AuditUtils.getEventTypeFromTaskType(req.getTaskType());
                auditEvent.setType(type);
                auditEvent.setAction(AuditUtils.getActualActionForTask(type, auditEventMeta.getAction()));
            }
        }
        return auditEvent;
    }

    private String parseDetailFromApiParams(Map<String, Object> apiParams) {
        String jsonStr = JsonUtils.toJson(apiParams);
        // 如果是不能序列化的参数，需要特殊处理
        if (StringUtils.isEmpty(jsonStr)) {
            // 脚本上传，记录上传的文件名
            if (Objects.nonNull(apiParams.get("files"))) {
                List<MultipartFile> files = (List<MultipartFile>) apiParams.get("files");
                jsonStr = JsonUtils
                        .toJson(files.stream().map(MultipartFile::getOriginalFilename).collect(Collectors.toList()));
            }
            // audit_event 表的 detail 字段不能为空，除了脚本上传的场景，现在都是可序列化的。
            // 稳妥起见，这里返回一个指定字符串防止插入失败
            if (StringUtils.isEmpty(jsonStr)) {
                jsonStr = "No detail";
            }
        }
        // 防止参数过长，这里做一个截断
        return truncateDetail(jsonStr);
    }

    /**
     * Table audit_event.detail type is text, so we truncate input detail when it's length longer then
     * MAX_DETAIL_LENGTH
     * 
     * @param detail origin input detail
     * @return a truncated detail
     */
    private String truncateDetail(String detail) {
        return detail == null ? null : detail.substring(0, Math.min(detail.length(), MAX_DETAIL_LENGTH));
    }

    /**
     * parse actual action from api parameters
     */
    private AuditEventAction parseActualAction(AuditEventAction originAction, Map<String, Object> apiParams) {
        AuditEventAction actualAction = originAction;
        /**
         * Predicate if disable flow config
         */
        if (AuditEventAction.ENABLE_FLOW_CONFIG == originAction) {
            if (!((SetEnabledReq) apiParams.get("req")).getEnabled()) {
                actualAction = AuditEventAction.DISABLE_FLOW_CONFIG;
            }
        }
        /**
         * Predicate if disable user
         */
        if (AuditEventAction.ENABLE_USER == originAction) {
            if (!((SetEnabledReq) apiParams.get("req")).getEnabled()) {
                actualAction = AuditEventAction.DISABLE_USER;
            }
        }
        /**
         * Predicate if disable role
         */
        if (AuditEventAction.ENABLE_ROLE == originAction) {
            if (!((SetEnabledReq) apiParams.get("req")).getEnabled()) {
                actualAction = AuditEventAction.DISABLE_ROLE;
            }
        }
        /**
         * Predicate if disable resource group
         */
        if (AuditEventAction.ENABLE_RESOURCE_GROUP == originAction) {
            if (!((SetEnabledReq) apiParams.get("request")).getEnabled()) {
                actualAction = AuditEventAction.DISABLE_RESOURCE_GROUP;
            }
        }
        /**
         * Predicate if disable connection
         */
        if (AuditEventAction.ENABLE_CONNECTION == originAction) {
            if (!((SetEnabledReq) apiParams.get("req")).getEnabled()) {
                actualAction = AuditEventAction.DISABLE_CONNECTION;
            }
        }

        /**
         * Predicate if disable data masking rule
         */
        if (AuditEventAction.ENABLE_DATA_MASKING_RULE == originAction) {
            if (!((SetEnabledReq) apiParams.get("req")).getEnabled()) {
                actualAction = AuditEventAction.DISABLE_DATA_MASKING_RULE;
            }
        }
        return actualAction;
    }

    private <T> T parse(String expressionStr, Method method, Object[] args, Class<T> clazz) {
        String[] parameterNames = nameDiscoverer.getParameterNames(method);
        if (StringUtils.isEmpty(expressionStr) || Objects.isNull(parameterNames)
                || ArrayUtils.isEmpty(parameterNames)) {
            return null;
        }
        Expression expression = parser.parseExpression(expressionStr);
        EvaluationContext context = new StandardEvaluationContext();
        for (int idx = 0; idx < parameterNames.length; idx++) {
            context.setVariable(parameterNames[idx], args[idx]);
        }
        return expression.getValue(context, clazz);
    }


    private String parseSid(AuditEventMeta auditEventMeta, Method method, Object[] args) {
        String sid = parse(auditEventMeta.getSidExtractExpression(), method, args, String.class);
        // sid 为一个 list 的情况，由 ',' 隔开，这里获取 list 的第一个值即可
        if (StringUtils.isNotEmpty(sid)) {
            if (sid.contains(COMMA)) {
                sid = sid.split(COMMA)[0];
            }
        }
        return StringUtils.isNotBlank(sid) && !StringUtils.equalsIgnoreCase(sid, "null") ? sid : StringUtils.EMPTY;
    }

    private Long parseDatabaseId(AuditEventMeta auditEventMeta, Method method, Object[] args) {
        return parse(auditEventMeta.getDatabaseIdExtractExpression(), method, args, Long.class);
    }

    /**
     * 从方法中获取参数列表，存入一个 Map 中
     *
     * @param method
     * @param args 方法的参数值
     * @return 参数名 -> 参数值的映射关系
     */
    private Map<String, Object> getApiParams(Method method, Object[] args) {
        Map<String, Object> apiParams = new HashMap<>();
        String[] paraNameArr = nameDiscoverer.getParameterNames(method);
        if (Objects.nonNull(paraNameArr)) {
            IntStream.range(0, paraNameArr.length).forEach(idx -> apiParams.put(paraNameArr[idx], args[idx]));
        }
        return apiParams.isEmpty() ? null : apiParams;
    }

    private String getMethodSignature(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }

    private AuditEvent setConnectionRelatedProperties(AuditEvent auditEvent, String sid) {
        /**
         * If sid represents static connectionId
         */
        if (!StringUtils.contains(sid, "sid:")) {
            ConnectionConfig config = connectionService.getWithoutPermissionCheck(Long.parseLong(sid));
            if (Objects.nonNull(config)) {
                fillAuditEventByConnectionConfig(auditEvent, config);
            }
        } else {
            /**
             * If sid represents dynamic session id
             */
            ConnectionSession session = sessionService.nullSafeGet(SidUtils.getSessionId(sid));
            Object value = ConnectionSessionUtil.getConnectionConfig(session);
            if (value instanceof ConnectionConfig) {
                ConnectionConfig config = (ConnectionConfig) value;
                fillAuditEventByConnectionConfig(auditEvent, config);
            }
        }
        return auditEvent;
    }

    private void fillAuditEventByConnectionConfig(AuditEvent auditEvent, ConnectionConfig config) {
        auditEvent.setConnectionName(config.getName());
        auditEvent.setConnectionClusterName(config.getClusterName());
        auditEvent.setConnectionTenantName(config.getTenantName());
        auditEvent.setConnectionDialectType(config.getDialectType());
        auditEvent.setConnectionHost(config.getHost());
        auditEvent.setConnectionPort(config.getPort());
        auditEvent.setConnectionId(config.getId());
        auditEvent.setConnectionUsername(config.getUsername());
    }
}
