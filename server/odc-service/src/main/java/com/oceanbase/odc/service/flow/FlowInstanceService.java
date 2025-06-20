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
package com.oceanbase.odc.service.flow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.i18n.I18n;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.common.task.RouteLogCallable;
import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.collaboration.EnvironmentEntity;
import com.oceanbase.odc.metadb.collaboration.EnvironmentRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository.FlowInstanceProjection;
import com.oceanbase.odc.metadb.flow.FlowInstanceSpecs;
import com.oceanbase.odc.metadb.flow.FlowInstanceViewEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceViewRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceViewSpecs;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceSpecs;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.project.ProjectService;
import com.oceanbase.odc.service.common.FutureCache;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.connection.CloudMetadataClient;
import com.oceanbase.odc.service.connection.CloudMetadataClient.CloudPermissionAction;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.DBResource;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.UnauthorizedDBResource;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.OBTenant;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangeDatabase;
import com.oceanbase.odc.service.databasechange.model.DatabaseChangingRecord;
import com.oceanbase.odc.service.dispatch.DispatchResponse;
import com.oceanbase.odc.service.dispatch.RequestDispatcher;
import com.oceanbase.odc.service.dispatch.TaskDispatchChecker;
import com.oceanbase.odc.service.flow.factory.FlowFactory;
import com.oceanbase.odc.service.flow.factory.FlowResponseMapperFactory;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstanceConfigurer;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.listener.AutoApproveUserTaskListener;
import com.oceanbase.odc.service.flow.model.BatchTerminateFlowResult;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp.FlowInstanceMapper;
import com.oceanbase.odc.service.flow.model.FlowMetaInfo;
import com.oceanbase.odc.service.flow.model.FlowNodeInstanceDetailResp.FlowNodeInstanceMapper;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.model.InnerQueryFlowInstanceParams;
import com.oceanbase.odc.service.flow.model.QueryFlowInstanceParams;
import com.oceanbase.odc.service.flow.processor.EnablePreprocess;
import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonParameter;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.MultipleDatabaseChangeTaskResult;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskParameter;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.ProjectPermissionValidator;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.iam.model.UserResourceRole;
import com.oceanbase.odc.service.iam.util.SecurityContextUtils;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.client.ApprovalClient;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;
import com.oceanbase.odc.service.monitor.DefaultMeterName;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterManager;
import com.oceanbase.odc.service.notification.Broker;
import com.oceanbase.odc.service.notification.NotificationProperties;
import com.oceanbase.odc.service.notification.helper.EventBuilder;
import com.oceanbase.odc.service.notification.model.Event;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter;
import com.oceanbase.odc.service.permission.database.model.ApplyDatabaseParameter.ApplyDatabase;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.table.model.ApplyTableParameter;
import com.oceanbase.odc.service.permission.table.model.ApplyTableParameter.ApplyTable;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalNodeConfig;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriberIdentifier;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.state.StatefulUuidStateIdGenerator;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.base.precheck.PreCheckRiskLevel;
import com.oceanbase.odc.service.task.executor.logger.LogUtils;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.service.SpringTransactionManager;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import io.micrometer.core.instrument.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2022/2/9
 */
@Service
@Slf4j
@Validated
@SkipAuthorize("flow instance use internal check")
public class FlowInstanceService {

    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private FlowFactory flowFactory;
    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;
    @Autowired
    private FlowTaskProperties flowTaskProperties;
    @Autowired
    private ApprovalPermissionService approvalPermissionService;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private FlowResponseMapperFactory mapperFactory;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private TaskDispatchChecker dispatchChecker;
    @Autowired
    private RequestDispatcher requestDispatcher;
    private ActiveTaskAccessor activeTaskAccessor;
    @Autowired
    @Qualifier("autoApprovalExecutor")
    private ThreadPoolTaskExecutor executorService;
    @Autowired
    private ApprovalClient approvalClient;
    @Autowired
    private IntegrationService integrationService;
    @Autowired
    private SystemConfigService systemConfigService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RiskLevelService riskLevelService;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private FlowInstanceViewRepository flowInstanceViewRepository;
    @Autowired
    private ProjectPermissionValidator projectPermissionValidator;
    @Autowired
    private ResourceRoleService resourceRoleService;
    @Autowired
    private DBResourcePermissionHelper permissionHelper;
    @Autowired
    private NotificationProperties notificationProperties;
    @Autowired
    private Broker broker;
    @Autowired
    private EventBuilder eventBuilder;
    @Autowired
    private CloudMetadataClient cloudMetadataClient;
    @Autowired
    private EnvironmentRepository environmentRepository;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private StatefulUuidStateIdGenerator statefulUuidStateIdGenerator;
    @Autowired
    private ThreadPoolTaskExecutor commonAsyncTaskExecutor;
    @Autowired
    private FutureCache futureCache;
    @Autowired
    private FlowPermissionHelper flowPermissionHelper;
    @Autowired
    private MeterManager meterManager;
    @Autowired
    @Lazy
    private ApprovalFlowConfigSelector approvalFlowConfigSelector;
    @Autowired
    @Lazy
    private ProjectService projectService;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Value("${odc.log.directory:./log}")
    private String logPath;
    private static final long MAX_EXPORT_OBJECT_COUNT = 10000;
    private static final String ODC_SITE_URL = "odc.site.url";
    private static final int MAX_APPLY_DATABASE_SIZE = 10;
    private final List<Consumer<DataTransferTaskInitEvent>> dataTransferTaskInitHooks = new ArrayList<>();
    private final List<Consumer<ShadowTableComparingUpdateEvent>> shadowTableComparingTaskHooks = new ArrayList<>();

    @PostConstruct
    public void init() {
        this.eventPublisher.addEventListener(new AutoApproveUserTaskListener(executorService));
        this.activeTaskAccessor = new DefaultActiveTaskAccessor(eventPublisher);
    }

    @PreDestroy
    public void destroy() {
        try {
            Set<Long> flowInstanceIds = this.activeTaskAccessor.getActiveTasks().stream().map(
                    BaseRuntimeFlowableDelegate::getFlowInstanceId).collect(Collectors.toSet());
            if (CollectionUtils.isEmpty(flowInstanceIds)) {
                return;
            }
            flowInstanceRepository.updateStatusByIds(flowInstanceIds, FlowStatus.CANCELLED);
            log.info("Application is closing, changing the state of the flow succeeded, flowInstanceIds={}, status={}",
                    flowInstanceIds, FlowStatus.CANCELLED);
        } catch (Exception e) {
            log.warn("Application is closing, failed to change process instance state", e);
        }
    }

    @EnablePreprocess
    @Transactional
    public List<FlowInstanceDetailResp> createWithoutApprovalNode(CreateFlowInstanceReq createReq) {
        Long connId = createReq.getConnectionId();
        ConnectionConfig conn = connectionService.getForConnectionSkipPermissionCheck(connId);
        return Collections.singletonList(buildWithoutApprovalNode(createReq, conn));
    }

    @EnablePreprocess
    @Transactional
    public List<FlowInstanceDetailResp> createIndividualFlowInstance(CreateFlowInstanceReq createReq) {
        Long connId = createReq.getConnectionId();
        ConnectionConfig conn = connectionService.getForConnect(connId);
        cloudMetadataClient.checkPermission(OBTenant.of(conn.getClusterName(),
                conn.getTenantName()), conn.getInstanceType(), false, CloudPermissionAction.READONLY);
        return Collections.singletonList(buildWithoutApprovalNode(createReq, conn));
    }

    /**
     * inner create alter schedule flow instance
     */
    @EnablePreprocess
    @Transactional
    public Long createAlterSchedule(CreateFlowInstanceReq createReq) {
        List<RiskLevel> riskLevels = riskLevelService.list();
        Verify.notEmpty(riskLevels, "riskLevels");
        FlowInstanceDetailResp flowInstanceDetailResp =
                buildFlowInstance(riskLevels, createReq, Collections.emptyList());
        return flowInstanceDetailResp.getId();
    }

    private Map<Long, RiskLevel> getDbMappingRiskLevel(@NonNull @Valid CreateFlowInstanceReq createReq) {
        Set<Long> dbIds = Collections.emptySet();
        if (createReq.getTaskType() == TaskType.APPLY_DATABASE_PERMISSION) {
            final ApplyDatabaseParameter applyDatabaseParameter = (ApplyDatabaseParameter) (createReq.getParameters());
            dbIds = applyDatabaseParameter.getDatabases().stream()
                    .map(ApplyDatabase::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } else if (createReq.getTaskType() == TaskType.APPLY_TABLE_PERMISSION) {
            final ApplyTableParameter applyDatabaseParameter = (ApplyTableParameter) (createReq.getParameters());
            dbIds = applyDatabaseParameter.getTables().stream()
                    .map(ApplyTable::getDatabaseId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (CollectionUtils.isEmpty(dbIds)) {
            throw new IllegalStateException("databaseIds is not supposed to be empty");
        }
        Set<RiskLevelDescriberIdentifier> RiskLevelDescriberIdentifiers = databaseService
                .listDatabasesDetailsByIds(dbIds).stream().map(
                        d -> RiskLevelDescriberIdentifier.of(d.getId(),
                                RiskLevelDescriber.of(d, createReq.getTaskType().name())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<RiskLevelDescriberIdentifier, RiskLevel> RiskLevelDescriberIdentifier2MaxRiskLevel =
                approvalFlowConfigSelector.batchSelect(RiskLevelDescriberIdentifiers);

        Map<Long, RiskLevel> dbId2RiskLevel = new HashMap<>();
        RiskLevel defultHighestRiskLevel = null;
        for (Entry<RiskLevelDescriberIdentifier, RiskLevel> entry : RiskLevelDescriberIdentifier2MaxRiskLevel
                .entrySet()) {
            defultHighestRiskLevel =
                    ObjectUtil.defaultIfNull(defultHighestRiskLevel,
                            riskLevelService.findHighestRiskLevel());
            dbId2RiskLevel.put(entry.getKey().getDatabaseId(), entry.getValue());
        }
        return dbId2RiskLevel;
    }

    private List<String> listCandidateIdentifiers(@NonNull ApprovalFlowConfig config,
            @NonNull Supplier<Long> dbIdSupplier, @NonNull Supplier<Long> projectIdSupplier,
            @NonNull Map<Long, ResourceRoleEntity> resourceRoleId2ResourceRole) {
        List<String> candidateIdentifier = new ArrayList<>();
        for (ApprovalNodeConfig node : config.getNodes()) {
            if (node.getResourceRoleId() != null) {
                ResourceRoleEntity resourceRole = resourceRoleId2ResourceRole.get(node.getResourceRoleId());
                if (resourceRole != null && resourceRole.getResourceType() == ResourceType.ODC_DATABASE) {
                    candidateIdentifier.add(dbIdSupplier.get() + ":" + node.getResourceRoleId());
                } else {
                    candidateIdentifier.add(projectIdSupplier.get() + ":" + node.getResourceRoleId());
                }
            }
        }
        return candidateIdentifier;
    }

    private Map<Long, List<Set<Long>>> getDbIdMappingApprovalUserIds(
            @NonNull Map<Long, List<String>> dbId2CandidateIdentifier) {
        Map<Long, List<Set<Long>>> dbId2ApprovalUserIds = new HashMap<>();
        Set<String> toQueryResourceIdentifiers = dbId2CandidateIdentifier.values().stream().flatMap(List::stream)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(toQueryResourceIdentifiers)) {
            return dbId2ApprovalUserIds;
        }
        Map<String, Set<Long>> resourceIdentifier2ApprovalUserIds =
                resourceRoleService.listByResourceIdentifierIn(toQueryResourceIdentifiers)
                        .stream()
                        .collect(Collectors.groupingBy(r -> r.getResourceId() + ":" + r.getResourceRoleId(),
                                Collectors.mapping(UserResourceRole::getUserId, Collectors.toSet())));
        for (Entry<Long, List<String>> dbId2CandidateIdentifierEntry : dbId2CandidateIdentifier.entrySet()) {
            Long dbId = dbId2CandidateIdentifierEntry.getKey();
            for (String identifier : dbId2CandidateIdentifierEntry.getValue()) {
                dbId2ApprovalUserIds.computeIfAbsent(dbId, k -> new ArrayList<>())
                        .add(resourceIdentifier2ApprovalUserIds.getOrDefault(identifier, Collections.emptySet()));
            }
        }
        return dbId2ApprovalUserIds;
    }

    public List<MergedDbCreatedData> mergeDbWhenApplyPermissionOfDbOrTable(
            @NonNull @Valid CreateFlowInstanceReq createReq) {
        if (createReq.getTaskType() != TaskType.APPLY_DATABASE_PERMISSION
                && createReq.getTaskType() != TaskType.APPLY_TABLE_PERMISSION) {
            throw new UnsupportedException("Only APPLY_DATABASE_PERMISSION or APPLY_TABLE_PERMISSION are supported");
        }
        Map<Long, RiskLevel> dbId2RiskLevel = getDbMappingRiskLevel(createReq);
        Set<Long> allResourceRoleIds = dbId2RiskLevel.values().stream().map(
                r -> r.getApprovalFlowConfig().getNodes().stream().map(
                        ApprovalNodeConfig::getResourceRoleId).collect(Collectors.toSet()))
                .flatMap(Set::stream).collect(Collectors.toSet());
        Map<Long, ResourceRoleEntity> resourceRoleId2ResourceRole =
                resourceRoleService.listResourceRoleByIds(allResourceRoleIds).stream()
                        .collect(Collectors.toMap(ResourceRoleEntity::getId, v -> v, (e, r) -> e));
        Map<Long, List<String>> dbId2CandidateIdentifier = new HashMap<>();
        for (Entry<Long, RiskLevel> dbId2RiskLevelEntry : dbId2RiskLevel.entrySet()) {
            ApprovalFlowConfig approvalFlowConfig = dbId2RiskLevelEntry.getValue().getApprovalFlowConfig();
            dbId2CandidateIdentifier.put(dbId2RiskLevelEntry.getKey(), listCandidateIdentifiers(approvalFlowConfig,
                    dbId2RiskLevelEntry::getKey,
                    createReq::getProjectId,
                    resourceRoleId2ResourceRole));
        }
        Map<Long, List<Set<Long>>> dbId2ApprovalUserIds = getDbIdMappingApprovalUserIds(dbId2CandidateIdentifier);
        Map<MergedDbIdentifier, MergedDbCreatedData> mergedMap = new HashMap<>();
        for (Entry<Long, List<String>> dbId2CandidateIdentifierEntry : dbId2CandidateIdentifier.entrySet()) {
            Long dbId = dbId2CandidateIdentifierEntry.getKey();
            RiskLevel riskLevel = dbId2RiskLevel.get(dbId);
            MergedDbIdentifier mergedDbIdentifier =
                    new MergedDbIdentifier(dbId2ApprovalUserIds.get(dbId), riskLevel.getId());
            if (!mergedMap.containsKey(mergedDbIdentifier)) {
                mergedMap.put(mergedDbIdentifier, new MergedDbCreatedData(riskLevel, Sets.newHashSet(dbId)));
            } else {
                mergedMap.get(mergedDbIdentifier).getCandidateResourceIds().add(dbId);
            }
        }
        return new ArrayList<>(mergedMap.values());
    }

    @EnablePreprocess
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    public List<FlowInstanceDetailResp> create(@NotNull @Valid CreateFlowInstanceReq createReq) {
        if (createReq.getTaskType() == TaskType.APPLY_DATABASE_PERMISSION) {
            ApplyDatabaseParameter parameter = (ApplyDatabaseParameter) createReq.getParameters();
            List<ApplyDatabase> databases = new ArrayList<>(parameter.getDatabases());
            if (CollectionUtils.isNotEmpty(databases) && databases.size() > MAX_APPLY_DATABASE_SIZE) {
                throw new IllegalStateException("The number of databases to apply for exceeds the maximum limit");
            }
            List<MergedDbCreatedData> mergedDbCreatedDataList =
                    mergeDbWhenApplyPermissionOfDbOrTable(createReq);
            return mergedDbCreatedDataList.stream().map(e -> {
                List<ApplyDatabase> applyDatabases = databases.stream().filter(
                        d -> e.getCandidateResourceIds().contains(d.getId())).collect(Collectors.toList());
                parameter.setDatabases(applyDatabases);
                parameter.setRiskLevel(PreCheckRiskLevel.from(e.getRiskLevel()));
                createReq.setParameters(parameter);
                return innerCreateWithRiskLevel(createReq, e.getRiskLevel());
            }).collect(Collectors.toList());
        } else if (createReq.getTaskType() == TaskType.APPLY_TABLE_PERMISSION) {
            ApplyTableParameter parameter = (ApplyTableParameter) createReq.getParameters();
            List<ApplyTable> tables = new ArrayList<>(parameter.getTables());
            Map<Long, List<ApplyTable>> databaseId2Tables =
                    tables.stream().collect(Collectors.groupingBy(ApplyTable::getDatabaseId));
            if (CollectionUtils.isNotEmpty(databaseId2Tables.keySet())
                    && databaseId2Tables.keySet().size() > MAX_APPLY_DATABASE_SIZE) {
                throw new IllegalStateException("The number of databases to apply for exceeds the maximum limit");
            }
            List<MergedDbCreatedData> mergedDbCreatedDataList =
                    mergeDbWhenApplyPermissionOfDbOrTable(createReq);
            return mergedDbCreatedDataList.stream().map(e -> {
                List<ApplyTable> applyTables = tables.stream().filter(
                        d -> e.getCandidateResourceIds().contains(d.getDatabaseId())).collect(Collectors.toList());
                parameter.setTables(applyTables);
                parameter.setRiskLevel(PreCheckRiskLevel.from(e.getRiskLevel()));
                createReq.setParameters(parameter);
                return innerCreateWithRiskLevel(createReq, e.getRiskLevel());
            }).collect(Collectors.toList());
        } else {
            return innerCreate(createReq);
        }
    }

    private FlowInstanceDetailResp innerCreateWithRiskLevel(@NonNull @Valid CreateFlowInstanceReq createReq,
            @NonNull RiskLevel riskLevel) {
        checkCreateFlowInstancePermission(createReq);
        List<ConnectionConfig> conns = new ArrayList<>();
        if (Objects.nonNull(createReq.getConnectionId())) {
            ConnectionConfig conn = connectionService.getForConnectionSkipPermissionCheck(createReq.getConnectionId());
            cloudMetadataClient.checkPermission(OBTenant.of(conn.getClusterName(),
                    conn.getTenantName()), conn.getInstanceType(), false, CloudPermissionAction.READONLY);
            conns.add(conn);
        }
        return buildFlowInstance(Collections.singletonList(riskLevel), createReq, conns);
    }

    private List<FlowInstanceDetailResp> innerCreate(@NotNull @Valid CreateFlowInstanceReq createReq) {
        checkCreateFlowInstancePermission(createReq);
        if (createReq.getTaskType() == TaskType.ASYNC) {
            DatabaseChangeParameters taskParameters = (DatabaseChangeParameters) createReq.getParameters();
            PreConditions.maxLength(taskParameters.getSqlContent(), "sql content",
                    flowTaskProperties.getSqlContentMaxLength());
        }
        if (createReq.getTaskType() == TaskType.EXPORT) {
            DataTransferConfig dataTransferConfig = (DataTransferConfig) createReq.getParameters();
            if (dataTransferConfig.getExportDbObjects().size() > MAX_EXPORT_OBJECT_COUNT) {
                throw new OverLimitException(LimitMetric.EXPORT_OBJECT_COUNT, (double) MAX_EXPORT_OBJECT_COUNT,
                        String.format("export object has exceeded max size limit: %s", MAX_EXPORT_OBJECT_COUNT));
            }
            createReq.setParameters(dataTransferConfig);
        }
        List<RiskLevel> riskLevels = riskLevelService.list();
        Verify.notEmpty(riskLevels, "riskLevels");
        List<ConnectionConfig> conns = new ArrayList<>();
        if (Objects.nonNull(createReq.getConnectionId())) {
            ConnectionConfig conn = connectionService.getForConnectionSkipPermissionCheck(createReq.getConnectionId());
            cloudMetadataClient.checkPermission(OBTenant.of(conn.getClusterName(),
                    conn.getTenantName()), conn.getInstanceType(), false, CloudPermissionAction.READONLY);
            conns.add(conn);
        } else if (createReq.getTaskType() == TaskType.MULTIPLE_ASYNC) {
            MultipleDatabaseChangeParameters taskParameters =
                    (MultipleDatabaseChangeParameters) createReq.getParameters();
            // Gets the data source connection collection that contains the password
            List<Long> dataSourceIds =
                    taskParameters.getDatabases().stream().map(x -> x.getDataSource().getId()).distinct()
                            .collect(Collectors.toList());
            conns = connectionService.listForConnectionSkipPermissionCheck(dataSourceIds);
            conns.forEach(con -> cloudMetadataClient.checkPermission(OBTenant.of(con.getClusterName(),
                    con.getTenantName()), con.getInstanceType(), false, CloudPermissionAction.READONLY));
        }
        return Collections.singletonList(buildFlowInstance(riskLevels, createReq, conns));
    }

    public Page<FlowInstanceDetailResp> list(@NotNull Pageable pageable, @NotNull QueryFlowInstanceParams params) {
        Page<FlowInstanceEntity> returnValue = listAll(pageable, params);
        if (returnValue.isEmpty()) {
            return Page.empty();
        }
        FlowInstanceMapper mapper = mapperFactory.generateMapperByEntities(returnValue.getContent(), false);
        return returnValue.map(mapper::map);
    }

    public FlowMetaInfo getMetaInfo() {
        List<UserTaskInstanceEntity> entities = approvalPermissionService.getApprovableApprovalInstances().stream()
                .filter(entity -> entity.getStatus() == FlowNodeStatus.EXECUTING).collect(Collectors.toList());
        return FlowMetaInfo.of(entities);
    }

    public Page<FlowInstanceDetailResp> listUnfinishedFlowInstances(@NotNull Pageable pageable,
            @NonNull Long projectId) {
        QueryFlowInstanceParams.builder().projectIds(Collections.singleton(projectId)).containsAll(true).statuses(
                Arrays.asList(FlowStatus.APPROVING, FlowStatus.CREATED, FlowStatus.EXECUTING, FlowStatus.ROLLBACKING,
                        FlowStatus.WAIT_FOR_EXECUTION, FlowStatus.WAIT_FOR_CONFIRM))
                .build();
        return list(pageable,
                QueryFlowInstanceParams.builder().projectIds(Collections.singleton(projectId)).containsAll(true)
                        .statuses(
                                FlowStatus.listUnfinishedStatus())
                        .build());
    }

    private Page<FlowInstanceEntity> listAll(@NotNull Pageable pageable, @NotNull QueryFlowInstanceParams params) {
        if (Objects.nonNull(params.getProjectIds())) {
            projectPermissionValidator.checkProjectRole(params.getProjectIds(), ResourceRoleName.all());
        }
        if (params.getParentInstanceId() != null) {
            // TODO 4.1.3 自动运行模块改造完成后剥离
            Set<Long> flowInstanceIds =
                    flowInstanceRepository.findByParentInstanceId(params.getParentInstanceId()).stream()
                            .map(FlowInstanceEntity::getId).collect(
                                    Collectors.toSet());
            if (flowInstanceIds.isEmpty()) {
                return Page.empty();
            }
            if (params.getType() != null) {
                List<ServiceTaskInstanceEntity> serviceTaskInstances = serviceTaskRepository.findByFlowInstanceIdIn(
                        flowInstanceIds);
                flowInstanceIds = serviceTaskInstances.stream().filter(o -> o.getTaskType() == params.getType())
                        .map(ServiceTaskInstanceEntity::getFlowInstanceId).collect(
                                Collectors.toSet());
            }
            if (flowInstanceIds.isEmpty()) {
                return Page.empty();
            }
            Specification<FlowInstanceEntity> specification =
                    Specification.where(FlowInstanceSpecs.idIn(flowInstanceIds))
                            .and(FlowInstanceSpecs.organizationIdEquals(authenticationFacade.currentOrganizationId()));
            if (CollectionUtils.isNotEmpty(params.getProjectIds())) {
                specification = specification.and(FlowInstanceSpecs.projectIdIn(params.getProjectIds()));
            }
            return flowInstanceRepository.findAll(specification, pageable);
        }

        List<Long> creatorIds = new LinkedList<>();
        if (StringUtils.isNotBlank(params.getCreator())) {
            creatorIds = userService.getUsersByFuzzyNameWithoutPermissionCheck(
                    params.getCreator()).stream().map(User::getId).collect(Collectors.toList());
        }
        Long targetId = null;
        if (StringUtils.isNumeric(params.getId())) {
            try {
                targetId = Long.valueOf(params.getId());
            } catch (Exception e) {
                log.warn("Failed to convert string id to number, params={}", params, e);
            }
        }

        Specification<FlowInstanceViewEntity> specification = Specification
                .where(FlowInstanceViewSpecs.creatorIdIn(creatorIds))
                .and(FlowInstanceViewSpecs.organizationIdEquals(authenticationFacade.currentOrganizationId()))
                .and(FlowInstanceViewSpecs.statusIn(params.getStatuses()))
                .and(FlowInstanceViewSpecs.createTimeLate(params.getStartTime()))
                .and(FlowInstanceViewSpecs.createTimeBefore(params.getEndTime()))
                .and(FlowInstanceViewSpecs.idEquals(targetId))
                .and(FlowInstanceViewSpecs.groupByIdAndTaskType());
        Set<TaskType> taskTypes;
        if (params.getType() != null) {
            taskTypes = Sets.newHashSet(params.getType());
            specification = specification.and(FlowInstanceViewSpecs.taskTypeEquals(params.getType()));
        } else {
            // Task type which will be filtered independently
            taskTypes = Sets.newHashSet(
                    TaskType.MULTIPLE_ASYNC,
                    TaskType.EXPORT,
                    TaskType.IMPORT,
                    TaskType.MOCKDATA,
                    TaskType.ASYNC,
                    TaskType.SHADOWTABLE_SYNC,
                    TaskType.PARTITION_PLAN,
                    TaskType.ONLINE_SCHEMA_CHANGE,
                    TaskType.EXPORT_RESULT_SET,
                    TaskType.APPLY_PROJECT_PERMISSION,
                    TaskType.APPLY_DATABASE_PERMISSION,
                    TaskType.STRUCTURE_COMPARISON,
                    TaskType.APPLY_TABLE_PERMISSION);
            specification = specification.and(FlowInstanceViewSpecs.taskTypeIn(taskTypes));
        }

        if (CollectionUtils.isNotEmpty(params.getProjectIds())) {
            specification = specification.and(FlowInstanceViewSpecs.projectIdIn(params.getProjectIds()));
        } else {
            Set<Long> joinedProjectIds = userService.getCurrentUserJoinedProjectIds();
            // If the user does not join any projects in team space, and it filters out the
            // APPLY_PROJECT_PERMISSION, then return empty directly
            if (CollectionUtils.isEmpty(joinedProjectIds)
                    && authenticationFacade.currentOrganization().getType() == OrganizationType.TEAM
                    && !taskTypes.contains(TaskType.APPLY_PROJECT_PERMISSION)) {
                return Page.empty();
            }
            // Add the condition of joined project ids or if it contains APPLY_PROJECT_PERMISSION, we only need
            // to require creatorId equals currentUserId because user should be allowed to view the
            // APPLY_PROJECT_PERMISSION tickets even if they have not joined that project
            specification =
                    specification.and(FlowInstanceViewSpecs.projectIdIn(userService.getCurrentUserJoinedProjectIds())
                            .or(FlowInstanceViewSpecs.creatorIdEquals(authenticationFacade.currentUserId())
                                    .and(FlowInstanceViewSpecs.taskTypeEquals(TaskType.APPLY_PROJECT_PERMISSION))));
        }
        if (params.getContainsAll()) {
            return flowInstanceViewRepository.findAll(specification, pageable).map(FlowInstanceEntity::from);
        }
        if (params.getCreatedByCurrentUser()) {
            // created by current user
            specification =
                    specification.and(FlowInstanceViewSpecs.creatorIdEquals(authenticationFacade.currentUserId()));
        }
        if (params.getApproveByCurrentUser()) {
            Set<String> resourceRoleIdentifiers = userService.getCurrentUserResourceRoleIdentifiers();
            // does not join any project, so there does not exist any tickets to approve
            if (CollectionUtils.isEmpty(resourceRoleIdentifiers)) {
                return Page.empty();
            }
            specification = specification.and(FlowInstanceViewSpecs.leftJoinFlowInstanceApprovalView(
                    resourceRoleIdentifiers, null, FlowNodeStatus.getExecutingStatuses()));
        }
        return flowInstanceViewRepository.findAll(specification, pageable).map(FlowInstanceEntity::from);
    }

    public List<FlowInstanceEntity> listByIds(@NonNull Collection<Long> ids) {
        return flowInstanceRepository.findByIdIn(ids);
    }

    public FlowInstanceDetailResp detail(@NotNull Long id) {
        return mapFlowInstanceWithReadPermission(id, flowInstance -> {
            FlowInstanceMapper instanceMapper = mapperFactory.generateMapperByInstance(flowInstance, false);
            FlowNodeInstanceMapper nodeInstanceMapper = mapperFactory.generateNodeMapperByInstance(flowInstance, false);
            return instanceMapper.map(flowInstance, nodeInstanceMapper);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public FlowInstanceDetailResp cancelWithWritePermission(@NotNull Long id, Boolean skipApproveAuth) {
        FlowInstance flowInstance = mapFlowInstanceWithWritePermission(id, flowInst -> flowInst);
        return cancel(flowInstance, skipApproveAuth);
    }

    @Transactional(rollbackFor = Exception.class)
    public FlowInstanceDetailResp cancelWithoutPermission(@NotNull Long id) {
        FlowInstance flowInstance = mapFlowInstanceWithoutPermissionCheck(id, flowInst -> flowInst);
        return cancel(flowInstance, true);
    }

    public String startBatchCancelFlowInstance(Collection<Long> flowInstanceIds) {
        String terminateId = statefulUuidStateIdGenerator.generateCurrentUserIdStateId("BatchFlowTerminate");
        User user = authenticationFacade.currentUser();
        Future<List<BatchTerminateFlowResult>> future = commonAsyncTaskExecutor.submit(
                new RouteLogCallable<List<BatchTerminateFlowResult>>("BatchFlowTerminate", terminateId, "terminate") {
                    @Override
                    public List<BatchTerminateFlowResult> doCall() {
                        SecurityContextUtils.setCurrentUser(user);
                        List<BatchTerminateFlowResult> results = new ArrayList<>();
                        for (Long id : flowInstanceIds) {
                            try {
                                new SpringTransactionManager(transactionTemplate)
                                        .doInTransactionWithoutResult(() -> cancelWithWritePermission(id, false));
                                results.add(BatchTerminateFlowResult.success(id));
                                log.info("Terminate flow success, flowInstanceId={}", id);
                            } catch (Exception e) {
                                log.info("Terminate flow failed, flowInstanceId={}", id, e);
                                results.add(BatchTerminateFlowResult.failed(id, e.getMessage()));
                            }
                        }
                        return results;
                    }
                });
        futureCache.put(terminateId, future);
        return terminateId;
    }

    public List<BatchTerminateFlowResult> getBatchCancelResult(String terminateId) {
        statefulUuidStateIdGenerator.checkCurrentUserId(terminateId);
        Future<List<BatchTerminateFlowResult>> future =
                (Future<List<BatchTerminateFlowResult>>) futureCache.get(terminateId);
        if (!future.isDone()) {
            return Collections.emptyList();
        }
        try {
            futureCache.invalid(terminateId);
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getBatchCancelLog(String terminateId) {
        statefulUuidStateIdGenerator.checkCurrentUserId(terminateId);
        return LogUtils.getRouteTaskLog(logPath, "BatchFlowTerminate", terminateId, "terminate");
    }

    public Map<Long, FlowStatus> getStatus(Set<Long> ids) {
        Specification<FlowInstanceEntity> specification = Specification.where(FlowInstanceSpecs.idIn(ids))
                .and(FlowInstanceSpecs.organizationIdEquals(authenticationFacade.currentOrganizationId()));
        List<FlowInstanceEntity> entities = flowInstanceRepository.findAll(specification);
        return entities.stream().collect(
                Collectors.toMap(FlowInstanceEntity::getId, FlowInstanceEntity::getStatus));
    }

    private FlowInstanceDetailResp cancel(@NotNull FlowInstance flowInstance, Boolean skipAuth) {
        long id = flowInstance.getId();
        Holder<TaskType> taskTypeHolder = new Holder<>();
        List<BaseFlowNodeInstance> instances = flowInstance.filterInstanceNode(instance -> {
            FlowNodeType nodeType = instance.getNodeType();
            if (nodeType != FlowNodeType.SERVICE_TASK && nodeType != FlowNodeType.APPROVAL_TASK) {
                return false;
            }
            if (instance instanceof FlowTaskInstance) {
                taskTypeHolder.setValue(((FlowTaskInstance) instance).getTaskType());
            }
            return instance.getStatus() == FlowNodeStatus.EXECUTING
                    || instance.getStatus() == FlowNodeStatus.PENDING;
        });
        Verify.notNull(taskTypeHolder.getValue(), "TaskType");
        List<FlowApprovalInstance> approvalInstances = instances.stream()
                .filter(instance -> instance.getNodeType() == FlowNodeType.APPROVAL_TASK).map(instance -> {
                    Verify.verify(instance instanceof FlowApprovalInstance, "FlowApprovalInstance's type is illegal");
                    return (FlowApprovalInstance) instance;
                }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(approvalInstances)) {
            Verify.singleton(approvalInstances, "FlowApprovalInstance");
            FlowApprovalInstance instance = approvalInstances.get(0);
            Verify.verify(instance.isPresentOnThisMachine(), "Approval instance is not on this machine");
            // Cancel external process instance when related ODC flow instance is cancelled
            cancelAllRelatedExternalInstance(flowInstance);
            instance.disApprove(null, !skipAuth);
            flowInstanceRepository.updateStatusById(instance.getFlowInstanceId(), FlowStatus.CANCELLED);
            userTaskInstanceRepository.updateStatusById(instance.getId(), FlowNodeStatus.CANCELLED);
            return FlowInstanceDetailResp.withIdAndType(id, taskTypeHolder.getValue());
        }

        List<FlowTaskInstance> taskInstances = instances.stream()
                .filter(instance -> instance.getNodeType() == FlowNodeType.SERVICE_TASK).map(instance -> {
                    Verify.verify(instance instanceof FlowTaskInstance, "FlowTaskInstance's type is illegal");
                    return (FlowTaskInstance) instance;
                }).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(taskInstances)) {
            Verify.singleton(taskInstances, "FlowTaskInstance");
            FlowTaskInstance taskInstance = taskInstances.get(0);
            if (taskInstance.getStatus() == FlowNodeStatus.PENDING) {
                taskInstance.abort();
                serviceTaskRepository.updateStatusById(taskInstance.getId(), FlowNodeStatus.CANCELLED);
                flowInstanceRepository.updateStatusById(taskInstance.getFlowInstanceId(), FlowStatus.CANCELLED);
                return FlowInstanceDetailResp.withIdAndType(id, taskInstance.getTaskType());
            }
            Long taskId = taskInstance.getTargetTaskId();
            if (taskId == null) {
                throw new UnsupportedException(ErrorCodes.FlowTaskNotSupportCancel,
                        new Object[] {taskTypeHolder.getValue().getLocalizedMessage()},
                        "The currently executing task does not support cancellation.");
            }
            TaskEntity taskEntity = taskService.detail(taskId);
            if (!dispatchChecker.isTaskEntityOnThisMachine(taskEntity)) {
                /**
                 * 任务不在当前机器上，需要进行 {@code RPC} 转发获取
                 */
                ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
                try {
                    DispatchResponse response =
                            requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
                    return response.getContentByType(
                            new TypeReference<SuccessResponse<FlowInstanceDetailResp>>() {}).getData();
                } catch (Exception e) {
                    log.warn("Remote termination task failed, flowInstanceId={}", id, e);
                }
                flowInstanceRepository.updateStatusById(id, FlowStatus.CANCELLED);
                return FlowInstanceDetailResp.withIdAndType(id, taskTypeHolder.getValue());
            }
            if (taskInstance.isPresentOnThisMachine()) {
                taskInstance.cancel(true);
                return FlowInstanceDetailResp.withIdAndType(id, taskInstance.getTaskType());
            }
        }
        if (CollectionUtils.isEmpty(approvalInstances) && CollectionUtils.isEmpty(taskInstances)) {
            // throw new UnsupportedException(ErrorCodes.FinishedTaskNotTerminable, null,
            // "The current task has been completed and cannot be terminated");
            log.warn("The current task has been completed, flowInstance = {}", flowInstance);
        }
        // check state before do update
        if (FlowStatus.CANCELLED != flowInstance.getStatus() && FlowStatus.COMPLETED != flowInstance.getStatus()) {
            log.info("Flow status error, cancellation conditions are not met, forced cancellation, flowInstanceId={}, "
                    + "nodes={}", id,
                    instances.stream().map(t -> t.getId() + "," + t.getNodeType() + "," + t.getStatus())
                            .collect(Collectors.toList()));
            flowInstanceRepository.updateStatusById(id, FlowStatus.CANCELLED);
        } else {
            log.warn("Flow status error, unexpected flow stats for cancellation, flowInstanceId={}, current status={}",
                    id, flowInstance.getId());
            throw new UnsupportedException(ErrorCodes.FinishedTaskNotTerminable, null,
                    "The current task has been completed and cannot be terminated");
        }
        return FlowInstanceDetailResp.withIdAndType(id, taskTypeHolder.getValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public FlowInstanceDetailResp approve(@NotNull Long id,
            @Size(max = 1024, message = "The approval comment is out of range [0,1024]") String message,
            Boolean skipAuth) throws IOException {
        TaskEntity taskEntity = getTaskByFlowInstanceId(id);
        if (taskEntity.getTaskType() == TaskType.IMPORT && !dispatchChecker.isTaskEntityOnThisMachine(taskEntity)) {
            /**
             * 对于导入任务，由于文件是上传到某台机器上的，因此任务的实际执行也一定要在那台机器上才行。 如果审批动作发送到了非任务所在机器，就需要转发，否则任务会因为找不到上传文件而报错。异步
             * 执行之所以没这个问题是因为接入了 {@code objectStorage} ，后续如果导入任务也接入了 {@code objectStorage} 则不用再有此逻辑。
             */
            ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
            DispatchResponse response = requestDispatcher.forward(executorInfo.getHost(), executorInfo.getPort());
            return response.getContentByType(new TypeReference<SuccessResponse<FlowInstanceDetailResp>>() {}).getData();
        }
        if (notificationProperties.isEnabled()) {
            try {
                Event event =
                        eventBuilder.ofApprovedTask(getTaskByFlowInstanceId(id), authenticationFacade.currentUserId());
                broker.enqueueEvent(event);
            } catch (Exception e) {
                log.warn("Failed to enqueue event.", e);
            }
        }
        completeApprovalInstance(id, instance -> instance.approve(message, !skipAuth), skipAuth);
        return FlowInstanceDetailResp.withIdAndType(id, taskEntity.getTaskType());
    }

    @Transactional(rollbackFor = Exception.class)
    public FlowInstanceDetailResp reject(@NotNull Long id,
            @Size(max = 1024, message = "The approval comment is out of range [0,1024]") String message,
            Boolean skipAuth) {
        if (notificationProperties.isEnabled()) {
            try {
                Event event =
                        eventBuilder.ofRejectedTask(getTaskByFlowInstanceId(id), authenticationFacade.currentUserId());
                broker.enqueueEvent(event);
            } catch (Exception e) {
                log.warn("Failed to enqueue event.", e);
            }
        }
        completeApprovalInstance(id, instance -> {
            instance.disApprove(message, !skipAuth);
            flowInstanceRepository.updateStatusById(instance.getFlowInstanceId(), FlowStatus.REJECTED);
        }, skipAuth);
        Optional<FlowInstance> optional = flowFactory.getFlowInstance(id);
        FlowInstance flowInstance =
                optional.orElseThrow(() -> new NotFoundException(ResourceType.ODC_FLOW_INSTANCE, "id", id));
        cancelAllRelatedExternalInstance(flowInstance);
        SpringContextUtil.getBean(ScheduleService.class).updateStatusByFlowInstanceId(id, ScheduleStatus.REJECTED);
        return FlowInstanceDetailResp.withIdAndType(id, getTaskByFlowInstanceId(id).getTaskType());
    }

    public <T> T mapFlowInstance(@NonNull Long flowInstanceId, Function<FlowInstance, T> mapper,
            Consumer<FlowInstance> checkAuth) {
        Optional<FlowInstance> optional = flowFactory.getFlowInstance(flowInstanceId);
        FlowInstance flowInstance =
                optional.orElseThrow(() -> new NotFoundException(ResourceType.ODC_FLOW_INSTANCE, "id", flowInstanceId));
        try {
            if (checkAuth != null) {
                checkAuth.accept(flowInstance);
            }
            return mapper.apply(flowInstance);
        } finally {
            flowInstance.dealloc();
        }
    }

    public <T> T mapFlowInstanceWithReadPermission(@NonNull Long flowInstanceId, Function<FlowInstance, T> mapper) {
        return mapFlowInstance(flowInstanceId, mapper, flowPermissionHelper.withProjectMemberCheck());
    }

    public <T> T mapFlowInstanceWithWritePermission(@NonNull Long flowInstanceId, Function<FlowInstance, T> mapper) {
        return mapFlowInstance(flowInstanceId, mapper, flowPermissionHelper.withExecutableCheck());
    }

    public <T> T mapFlowInstanceWithApprovalPermission(@NonNull Long flowInstanceId, Function<FlowInstance, T> mapper) {
        return mapFlowInstance(flowInstanceId, mapper, flowPermissionHelper.withApprovableCheck());
    }

    public <T> T mapFlowInstanceWithoutPermissionCheck(@NonNull Long flowInstanceId, Function<FlowInstance, T> mapper) {
        return mapFlowInstance(flowInstanceId, mapper, flowPermissionHelper.skipCheck());
    }

    public TaskEntity getTaskByFlowInstanceId(Long id) {
        List<ServiceTaskInstanceEntity> entities = serviceTaskRepository
                .findAll(ServiceTaskInstanceSpecs.flowInstanceIdEquals(id))
                .stream()
                .filter(e -> e.getTaskType() != TaskType.GENERATE_ROLLBACK && e.getTaskType() != TaskType.SQL_CHECK
                        && e.getTaskType() != TaskType.PRE_CHECK)
                .collect(Collectors.toList());
        Verify.verify(CollectionUtils.isNotEmpty(entities), "TaskEntities can not be empty");

        Set<Long> taskIds = entities.stream().filter(entity -> entity.getTargetTaskId() != null)
                .map(ServiceTaskInstanceEntity::getTargetTaskId).collect(Collectors.toSet());
        Verify.singleton(taskIds, "Multi task for one instance is not allowed, id " + id);
        Long taskId = taskIds.iterator().next();
        return taskService.detail(taskId);
    }

    public Map<Long, TaskEntity> getTaskByFlowInstanceIds(Collection<Long> flowInstanceIds) {
        List<ServiceTaskInstanceEntity> serviceTaskInstanceEntities = serviceTaskRepository
                .findByFlowInstanceIdIn(flowInstanceIds)
                .stream()
                .filter(e -> e.getTaskType() != TaskType.GENERATE_ROLLBACK && e.getTaskType() != TaskType.SQL_CHECK
                        && e.getTaskType() != TaskType.PRE_CHECK)
                .collect(Collectors.toList());
        Map<Long, Long> flowId2taskIds = serviceTaskInstanceEntities.stream().collect(
                Collectors.toMap(ServiceTaskInstanceEntity::getFlowInstanceId,
                        ServiceTaskInstanceEntity::getTargetTaskId, (exist, duplicate) -> exist));
        List<TaskEntity> taskEntities = taskService.findByIds(flowId2taskIds.values());
        Map<Long, TaskEntity> idTaskEntityMap =
                taskEntities.stream().collect(Collectors.toMap(TaskEntity::getId, t -> t));
        return flowId2taskIds.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, v -> idTaskEntityMap.get(v.getValue())));
    }

    private void checkCreateFlowInstancePermission(CreateFlowInstanceReq req) {
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return;
        }
        if (req.getTaskType() == TaskType.EXPORT) {
            DataTransferConfig parameters = (DataTransferConfig) req.getParameters();
            Map<DBResource, Set<DatabasePermissionType>> resource2Types = new HashMap<>();
            if (parameters.isExportAllObjects()) {
                ConnectionConfig config = connectionService.getBasicWithoutPermissionCheck(req.getConnectionId());
                resource2Types.put(
                        DBResource.from(config, req.getDatabaseName(), null, ResourceType.ODC_DATABASE),
                        DatabasePermissionType.from(TaskType.EXPORT));
            } else if (CollectionUtils.isNotEmpty(parameters.getExportDbObjects())) {
                ConnectionConfig config = connectionService.getBasicWithoutPermissionCheck(req.getConnectionId());
                parameters.getExportDbObjects().forEach(item -> {
                    if (item.getDbObjectType() == ObjectType.TABLE || item.getDbObjectType() == ObjectType.VIEW) {
                        resource2Types.put(
                                DBResource.from(config, req.getDatabaseName(), item.getObjectName(),
                                        ResourceType.ODC_TABLE),
                                DatabasePermissionType.from(TaskType.EXPORT));
                    }
                });
            }
            List<UnauthorizedDBResource> unauthorizedDBResources = this.permissionHelper
                    .filterUnauthorizedDBResources(resource2Types, false);
            if (CollectionUtils.isNotEmpty(unauthorizedDBResources)) {
                throw new BadRequestException(ErrorCodes.DatabaseAccessDenied,
                        new Object[] {unauthorizedDBResources.stream()
                                .map(UnauthorizedDBResource::getUnauthorizedPermissionTypes).flatMap(Collection::stream)
                                .map(DatabasePermissionType::getLocalizedMessage).collect(Collectors.joining(","))},
                        "Lack permission for the database with id " + req.getDatabaseId());
            }
            return;
        }
        Set<Long> databaseIds = new HashSet<>();
        if (Objects.nonNull(req.getDatabaseId())) {
            databaseIds.add(req.getDatabaseId());
        }
        TaskType taskType = req.getTaskType();
        if (taskType == TaskType.STRUCTURE_COMPARISON) {
            DBStructureComparisonParameter p = (DBStructureComparisonParameter) req.getParameters();
            databaseIds.add(p.getTargetDatabaseId());
            databaseIds.add(p.getSourceDatabaseId());
        } else if (taskType == TaskType.MULTIPLE_ASYNC) {
            MultipleDatabaseChangeParameters parameters = (MultipleDatabaseChangeParameters) req.getParameters();
            databaseIds =
                    parameters.getOrderedDatabaseIds().stream().flatMap(Collection::stream).collect(Collectors.toSet());
        }
        permissionHelper.checkDBPermissions(databaseIds, DatabasePermissionType.from(req.getTaskType()));
    }


    /**
     * It's used to internal build approval node.
     */
    private FlowInstanceDetailResp buildWithoutApprovalNode(CreateFlowInstanceReq flowInstanceReq,
            ConnectionConfig connectionConfig) {
        log.info("Start creating flow instance, flowInstanceReq={}", flowInstanceReq);
        TaskType taskType = flowInstanceReq.getTaskType();
        TaskEntity taskEntity = taskService.create(flowInstanceReq, (int) TimeUnit.SECONDS
                .convert(flowTaskProperties.getDefaultExecutionExpirationIntervalHours(), TimeUnit.HOURS));
        Verify.notNull(taskEntity.getId(), "TaskId can not be null");
        FlowInstance flowInstance = flowFactory.generateFlowInstance(generateFlowInstanceName(flowInstanceReq),
                flowInstanceReq.getParentFlowInstanceId(),
                flowInstanceReq.getProjectId(), flowInstanceReq.getDescription());
        Verify.notNull(flowInstance.getId(), "FlowInstance id can not be null");
        ExecutionStrategyConfig strategyConfig = ExecutionStrategyConfig.from(flowInstanceReq,
                ExecutionStrategyConfig.INVALID_EXPIRE_INTERVAL_SECOND);
        try {
            TaskParameters parameters = flowInstanceReq.getParameters();
            FlowInstanceConfigurer taskConfigurer;
            boolean addRollbackPlanNode = (taskType == TaskType.ASYNC
                    && Boolean.TRUE.equals(((DatabaseChangeParameters) parameters).getGenerateRollbackPlan()));
            FlowTaskInstance taskInstance =
                    flowFactory.generateFlowTaskInstance(flowInstance.getId(), !addRollbackPlanNode, true, taskType,
                            strategyConfig);
            taskInstance.setTargetTaskId(taskEntity.getId());
            if (addRollbackPlanNode) {
                FlowTaskInstance rollbackPlanInstance =
                        flowFactory.generateFlowTaskInstance(flowInstance.getId(), true, false,
                                TaskType.GENERATE_ROLLBACK, ExecutionStrategyConfig.autoStrategy());
                taskConfigurer = flowInstance.newFlowInstance().next(rollbackPlanInstance).next(taskInstance);
            } else {
                taskConfigurer = flowInstance.newFlowInstance().next(taskInstance);
            }
            taskConfigurer.endFlowInstance();
            flowInstance.buildTopology();

            Map<String, Object> variables = new HashMap<>();
            FlowTaskUtil.setTemplateVariables(variables, buildTemplateVariables(flowInstanceReq, connectionConfig));
            FlowTaskUtil.setFlowInstanceId(variables, flowInstance.getId());
            initVariables(variables, taskEntity, null, Collections.singletonList(connectionConfig),
                    buildRiskLevelDescriber(flowInstanceReq));
            flowInstance.start(variables);
            if (taskType == TaskType.SHADOWTABLE_SYNC) {
                consumeShadowTableHook((ShadowTableSyncTaskParameter) flowInstanceReq.getParameters(),
                        flowInstance.getId());
            } else if (taskType == TaskType.EXPORT) {
                consumeDataTransferHook((DataTransferConfig) flowInstanceReq.getParameters(), taskEntity.getId());
            }
            log.info("New flow instance succeeded, instanceId={}, flowInstanceReq={}",
                    flowInstance.getId(), flowInstanceReq);
            return FlowInstanceDetailResp.withIdAndType(flowInstance.getId(), flowInstanceReq.getTaskType());
        } catch (Exception e) {
            log.warn("Failed to build FlowInstance, flowInstanceReq={}", flowInstanceReq, e);
            throw e;
        } finally {
            flowInstance.dealloc();
        }
    }

    private FlowInstanceDetailResp buildFlowInstance(List<RiskLevel> riskLevels,
            CreateFlowInstanceReq flowInstanceReq, List<ConnectionConfig> connectionConfigs) {
        log.info("Start creating flow instance, flowInstanceReq={}", flowInstanceReq);
        CreateFlowInstanceReq preCheckReq = new CreateFlowInstanceReq();
        preCheckReq.setTaskType(TaskType.PRE_CHECK);
        preCheckReq.setConnectionId(flowInstanceReq.getConnectionId());
        preCheckReq.setDatabaseId(flowInstanceReq.getDatabaseId());
        preCheckReq.setDatabaseName(flowInstanceReq.getDatabaseName());
        TaskEntity preCheckTaskEntity = taskService.create(preCheckReq, (int) TimeUnit.SECONDS
                .convert(flowTaskProperties.getDefaultExecutionExpirationIntervalHours(), TimeUnit.HOURS));

        if (flowInstanceReq.getTaskType() == TaskType.APPLY_DATABASE_PERMISSION
                || flowInstanceReq.getTaskType() == TaskType.APPLY_TABLE_PERMISSION) {
            Verify.singleton(riskLevels, "RiskLevel");
            Verify.notNull(preCheckTaskEntity.getId(), "PreCheckTaskId");
        }

        TaskEntity taskEntity = taskService.create(flowInstanceReq, (int) TimeUnit.SECONDS
                .convert(flowTaskProperties.getDefaultExecutionExpirationIntervalHours(), TimeUnit.HOURS));
        Verify.notNull(taskEntity.getId(), "TaskId can not be null");
        FlowInstance flowInstance = flowFactory.generateFlowInstance(generateFlowInstanceName(flowInstanceReq),
                flowInstanceReq.getParentFlowInstanceId(),
                flowInstanceReq.getProjectId(), flowInstanceReq.getDescription());
        Verify.notNull(flowInstance.getId(), "FlowInstance id can not be null");

        try {
            FlowTaskInstance riskDetectInstance = flowFactory.generateFlowTaskInstance(flowInstance.getId(), true,
                    false, TaskType.PRE_CHECK,
                    ExecutionStrategyConfig.autoStrategy());
            riskDetectInstance.setTargetTaskId(preCheckTaskEntity.getId());
            FlowGatewayInstance riskLevelGateway =
                    flowFactory.generateFlowGatewayInstance(flowInstance.getId(), false, true);
            FlowInstanceConfigurer startConfigurer =
                    flowInstance.newFlowInstance().next(riskDetectInstance).next(riskLevelGateway);
            for (int i = 0; i < riskLevels.size(); i++) {
                FlowInstanceConfigurer targetConfigurer = buildConfigurer(riskLevels.get(i).getApprovalFlowConfig(),
                        flowInstance, flowInstanceReq.getTaskType(), taskEntity.getId(),
                        flowInstanceReq.getParameters(), flowInstanceReq);
                // Determining the risk level determines the route
                if (i == 0 && i == riskLevels.size() - 1) {
                    startConfigurer.route(targetConfigurer);
                } else {
                    startConfigurer.route(
                            String.format("${%s == %d}", RuntimeTaskConstants.RISKLEVEL, riskLevels.get(i).getLevel()),
                            targetConfigurer);
                }
            }
            flowInstance.buildTopology();
            flowInstanceReq.setId(flowInstance.getId());
        } catch (Exception e) {
            log.warn("Failed to build FlowInstance, flowInstanceReq={}", flowInstanceReq, e);
            throw e;
        } finally {
            flowInstance.dealloc();
        }
        Map<String, Object> variables = new HashMap<>();
        FlowTaskUtil.setFlowInstanceId(variables, flowInstance.getId());
        FlowTaskUtil.setTemplateVariables(variables, buildTemplateVariables(flowInstanceReq,
                CollectionUtils.isNotEmpty(connectionConfigs) ? connectionConfigs.get(0) : null));
        initVariables(variables, taskEntity, preCheckTaskEntity, connectionConfigs,
                buildRiskLevelDescriber(flowInstanceReq));
        flowInstance.start(variables);
        if (flowInstanceReq.getTaskType() == TaskType.SHADOWTABLE_SYNC) {
            consumeShadowTableHook((ShadowTableSyncTaskParameter) flowInstanceReq.getParameters(),
                    flowInstance.getId());
        } else if (flowInstanceReq.getTaskType() == TaskType.EXPORT) {
            consumeDataTransferHook((DataTransferConfig) flowInstanceReq.getParameters(), taskEntity.getId());
        } else if (flowInstanceReq.getTaskType() == TaskType.MULTIPLE_ASYNC) {
            generateMultipleDatabaseResult(flowInstanceReq, taskEntity);
        }
        log.info("New flow instance succeeded, instanceId={}, flowInstanceReq={}",
                flowInstance.getId(), flowInstanceReq);
        MeterKey meterKey = MeterKey.ofMeter(DefaultMeterName.FLOW_CREATED_COUNT,
                Tag.of("organizationId", flowInstance.getOrganizationId().toString()));
        meterManager.incrementCounter(meterKey);
        return FlowInstanceDetailResp.withIdAndType(flowInstance.getId(), flowInstanceReq.getTaskType());
    }

    private void generateMultipleDatabaseResult(CreateFlowInstanceReq flowInstanceReq, TaskEntity taskEntity) {
        MultipleDatabaseChangeParameters parameters =
                (MultipleDatabaseChangeParameters) flowInstanceReq.getParameters();
        MultipleDatabaseChangeTaskResult result = new MultipleDatabaseChangeTaskResult();
        List<DatabaseChangingRecord> databaseChangingRecords = new ArrayList<>();
        List<DatabaseChangeDatabase> databaseList = parameters.getDatabases();
        for (DatabaseChangeDatabase database : databaseList) {
            DatabaseChangingRecord databaseChangingRecord = new DatabaseChangingRecord();
            databaseChangingRecord.setDatabase(database);
            databaseChangingRecords.add(databaseChangingRecord);
        }
        result.setDatabaseChangingRecordList(databaseChangingRecords);
        taskEntity.setResultJson(JsonUtils.toJson(result));
        taskService.update(taskEntity);
    }

    private String generateFlowInstanceName(@NonNull CreateFlowInstanceReq req) {
        if (req.getTaskType() == TaskType.STRUCTURE_COMPARISON) {
            DBStructureComparisonParameter parameters = (DBStructureComparisonParameter) req.getParameters();
            return "structure_comparison_" + parameters.getSourceDatabaseId() + "_" + parameters.getTargetDatabaseId();
        }
        if (req.getTaskType() == TaskType.MULTIPLE_ASYNC) {
            MultipleDatabaseChangeParameters parameters = (MultipleDatabaseChangeParameters) req.getParameters();
            return "multiple_database_" + parameters.getOrderedDatabaseIds();
        }

        String schemaName = req.getDatabaseName();
        String connectionName = req.getConnectionId() == null ? "no_connection" : req.getConnectionId() + "";
        if (schemaName == null) {
            schemaName = "no_schema";
        }
        return schemaName + "_" + connectionName;
    }

    private Map<Long, Set<Long>> groupCandidateResourceIdsByResourceRoleId(@NonNull Collection<Long> resourceRoleIds,
            @NonNull CreateFlowInstanceReq flowInstanceReq) {
        Set<Long> resourceRoleIdSet = new HashSet<>(resourceRoleIds);
        if (CollectionUtils.isEmpty(resourceRoleIdSet)) {
            return Collections.emptyMap();
        }
        Map<Long, ResourceRoleEntity> resourceRoleId2ResourceRole = resourceRoleService
                .listResourceRoleByIds(resourceRoleIdSet)
                .stream().collect(Collectors.toMap(ResourceRoleEntity::getId, Function.identity(), (e, r) -> e));
        final Map<Long, Set<Long>> resourceRoleId2CandidateResourceIds = new HashMap<>();
        TaskParameters parameters = flowInstanceReq.getParameters();
        resourceRoleIdSet.forEach(resourceRoleId -> {
            Set<Long> candidateResourceIds = new HashSet<>();
            ResourceRoleEntity resourceRole = resourceRoleId2ResourceRole.get(resourceRoleId);
            if (resourceRole != null && resourceRole.getResourceType() == ResourceType.ODC_DATABASE) {
                if (flowInstanceReq.getDatabaseId() != null) {
                    candidateResourceIds.add(flowInstanceReq.getDatabaseId());
                }
                if (flowInstanceReq.getTaskType() == TaskType.MULTIPLE_ASYNC) {
                    candidateResourceIds
                            .addAll(((MultipleDatabaseChangeParameters) parameters).getOrderedDatabaseIds().stream()
                                    .flatMap(Collection::stream).collect(Collectors.toSet()));
                } else if (flowInstanceReq.getTaskType() == TaskType.APPLY_DATABASE_PERMISSION) {
                    candidateResourceIds
                            .addAll(((ApplyDatabaseParameter) parameters).getDatabases().stream()
                                    .map(ApplyDatabase::getId).collect(Collectors.toSet()));
                } else if (flowInstanceReq.getTaskType() == TaskType.APPLY_TABLE_PERMISSION) {
                    candidateResourceIds
                            .addAll(((ApplyTableParameter) parameters).getTables().stream()
                                    .map(ApplyTable::getDatabaseId).collect(Collectors.toSet()));
                }
            } else {
                candidateResourceIds.add(flowInstanceReq.getProjectId());
            }
            resourceRoleId2CandidateResourceIds.put(resourceRoleId,
                    candidateResourceIds.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
        });
        return resourceRoleId2CandidateResourceIds;
    }

    private FlowInstanceConfigurer buildConfigurer(
            @NonNull ApprovalFlowConfig approvalFlowConfig,
            @NonNull FlowInstance flowInstance,
            @NonNull TaskType taskType,
            @NonNull Long targetTaskId,
            @NonNull TaskParameters parameters,
            @NonNull CreateFlowInstanceReq flowInstanceReq) {
        List<ApprovalNodeConfig> nodeConfigs = approvalFlowConfig.getNodes();
        Verify.verify(!nodeConfigs.isEmpty(), "Approval Nodes size can not be equal to zero");
        List<FlowInstanceConfigurer> configurers = new LinkedList<>();
        Map<Long, Set<Long>> resourceRoleId2CandidateResourceIds = groupCandidateResourceIdsByResourceRoleId(
                nodeConfigs.stream().map(ApprovalNodeConfig::getResourceRoleId).collect(
                        Collectors.toSet()),
                flowInstanceReq);
        for (int nodeSequence = 0; nodeSequence < nodeConfigs.size(); nodeSequence++) {
            FlowInstanceConfigurer configurer;
            ApprovalNodeConfig nodeConfig = nodeConfigs.get(nodeSequence);
            Long resourceRoleId = nodeConfig.getResourceRoleId();
            FlowApprovalInstance approvalInstance = flowFactory.generateFlowApprovalInstance(flowInstance.getId(),
                    false, false,
                    nodeConfig.getAutoApproval(), approvalFlowConfig.getApprovalExpirationIntervalSeconds(),
                    nodeConfig.getExternalApprovalId());
            if (Objects.nonNull(resourceRoleId)) {
                Set<Long> candidateResourceIds =
                        resourceRoleId2CandidateResourceIds.getOrDefault(resourceRoleId, Collections.emptySet());
                approvalInstance.setCandidates(candidateResourceIds.stream().filter(Objects::nonNull)
                        .map(e -> StringUtils.join(e, ":", resourceRoleId)).collect(Collectors.toList()));
            }
            FlowGatewayInstance approvalGatewayInstance =
                    flowFactory.generateFlowGatewayInstance(flowInstance.getId(), false, true);
            configurer = flowInstance.newFlowInstanceConfigurer(approvalInstance);
            configurer = configurer.next(approvalGatewayInstance).route(String.format("${!%s}",
                    FlowApprovalInstance.APPROVAL_VARIABLE_NAME), flowInstance.endFlowInstance());
            if (nodeSequence == nodeConfigs.size() - 1) {
                if (taskType == TaskType.MULTIPLE_ASYNC) {
                    MultipleDatabaseChangeParameters multipleDatabaseChangeParameters =
                            (MultipleDatabaseChangeParameters) parameters;
                    FlowInstanceConfigurer taskConfigurer = null;
                    int orders = ((MultipleDatabaseChangeParameters) flowInstanceReq.getParameters())
                            .getOrderedDatabaseIds().size();
                    for (int i = 0; i < orders; i++) {
                        // ExecutionStrategyConfig for multiple databases change flow
                        ExecutionStrategyConfig strategyConfig = ExecutionStrategyConfig.from(flowInstanceReq,
                                Math.toIntExact(multipleDatabaseChangeParameters.getManualTimeoutMillis()) / 1000);
                        FlowTaskInstance taskInstance;
                        if (i == orders - 1) {
                            taskInstance = flowFactory.generateFlowTaskInstance(flowInstance.getId(), false, true,
                                    taskType, strategyConfig);
                        } else {
                            taskInstance = flowFactory.generateFlowTaskInstance(flowInstance.getId(), false, false,
                                    taskType, strategyConfig);
                        }

                        taskInstance.setTargetTaskId(targetTaskId);
                        if (taskConfigurer == null) {
                            taskConfigurer = flowInstance.newFlowInstanceConfigurer(taskInstance);
                        } else {
                            taskConfigurer.next(taskInstance);
                        }
                    }
                    taskConfigurer.endFlowInstance();
                    configurer.route(String.format("${%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME),
                            taskConfigurer);
                } else {
                    ExecutionStrategyConfig strategyConfig = ExecutionStrategyConfig.from(flowInstanceReq,
                            approvalFlowConfig.getWaitExecutionExpirationIntervalSeconds());
                    FlowTaskInstance taskInstance =
                            flowFactory.generateFlowTaskInstance(flowInstance.getId(), false, true,
                                    taskType, strategyConfig);
                    taskInstance.setTargetTaskId(targetTaskId);
                    FlowInstanceConfigurer taskConfigurer;
                    if (taskType == TaskType.ASYNC
                            && Boolean.TRUE.equals(((DatabaseChangeParameters) parameters).getGenerateRollbackPlan())) {
                        FlowTaskInstance rollbackPlanInstance =
                                flowFactory.generateFlowTaskInstance(flowInstance.getId(), false, false,
                                        TaskType.GENERATE_ROLLBACK, ExecutionStrategyConfig.autoStrategy());
                        taskConfigurer =
                                flowInstance.newFlowInstanceConfigurer(rollbackPlanInstance).next(taskInstance);
                    } else {
                        taskConfigurer = flowInstance.newFlowInstanceConfigurer(taskInstance);
                    }
                    taskConfigurer.endFlowInstance();
                    configurer.route(String.format("${%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME),
                            taskConfigurer);
                }

            }
            configurers.add(configurer);
        }
        for (int j = configurers.size() - 2; j >= 0; j--) {
            FlowInstanceConfigurer configurer = configurers.get(j);
            FlowInstanceConfigurer targetConfigurer = configurers.get(j + 1);
            configurer.route(String.format("${%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME), targetConfigurer);
        }
        return configurers.get(0);
    }

    private void completeApprovalInstance(@NonNull Long flowInstanceId,
            @NonNull Consumer<FlowApprovalInstance> consumer, Boolean skipAuth) {
        List<FlowApprovalInstance> instances =
                skipAuth ? mapFlowInstanceWithoutPermissionCheck(flowInstanceId,
                        generateApprovalMapper())
                        : mapFlowInstanceWithApprovalPermission(flowInstanceId, generateApprovalMapper());
        PreConditions.validExists(ResourceType.ODC_FLOW_APPROVAL_INSTANCE,
                "flowInstanceId", flowInstanceId, () -> instances.size() > 0);
        Verify.singleton(instances, "ApprovalInstance");
        FlowApprovalInstance target = instances.get(0);
        Verify.verify(target.isPresentOnThisMachine(), "Approval instance is not on this machine");
        consumer.accept(target);
    }

    private Function<FlowInstance, List<FlowApprovalInstance>> generateApprovalMapper() {
        return flowInstance -> flowInstance.filterInstanceNode(instance -> {
            if (instance.getNodeType() != FlowNodeType.APPROVAL_TASK) {
                return false;
            }
            return instance.getStatus() == FlowNodeStatus.EXECUTING
                    || instance.getStatus() == FlowNodeStatus.WAIT_FOR_CONFIRM;
        }).stream().map(instance -> {
            Verify.verify(instance instanceof FlowApprovalInstance,
                    "FlowApprovalInstance's type is illegal");
            return (FlowApprovalInstance) instance;
        }).collect(Collectors.toList());
    }

    private void initVariables(Map<String, Object> variables, TaskEntity taskEntity, TaskEntity preCheckTaskEntity,
            List<ConnectionConfig> configList, RiskLevelDescriber riskLevelDescriber) {
        FlowTaskUtil.setTaskId(variables, taskEntity.getId());
        if (Objects.nonNull(preCheckTaskEntity)) {
            FlowTaskUtil.setPreCheckTaskId(variables, preCheckTaskEntity.getId());
        }
        if (CollectionUtils.isNotEmpty(configList)) {
            FlowTaskUtil.setConnectionConfigList(variables, configList);
            FlowTaskUtil.setConnectionConfig(variables, configList.get(0));
        }
        FlowTaskUtil.setExecutionExpirationInterval(variables,
                taskEntity.getExecutionExpirationIntervalSeconds(), TimeUnit.SECONDS);
        FlowTaskUtil.setParameters(variables, taskEntity.getParametersJson());
        if (taskEntity.getDatabaseName() != null) {
            FlowTaskUtil.setSchemaName(variables, taskEntity.getDatabaseName());
        }
        if (taskEntity.getDatabaseId() != null) {
            FlowTaskUtil.setSchemaName(variables, databaseService.detail(taskEntity.getDatabaseId()).getName());
        }
        FlowTaskUtil.setTaskCreator(variables, authenticationFacade.currentUser());
        FlowTaskUtil.setOrganizationId(variables, authenticationFacade.currentOrganizationId());
        FlowTaskUtil.setTaskSubmitter(variables, JsonUtils.fromJson(taskEntity.getSubmitter(), ExecutorInfo.class));
        FlowTaskUtil.setRiskLevelDescriber(variables, riskLevelDescriber);
        FlowTaskUtil.setCloudMainAccountId(variables, authenticationFacade.currentUser().getParentUid());
    }

    private TemplateVariables buildTemplateVariables(CreateFlowInstanceReq flowInstanceReq, ConnectionConfig config) {
        TemplateVariables variables = new TemplateVariables();
        // set task url
        String odcTaskUrl = String.format("#/task?taskId=%d&taskType=%s&organizationId=%s", flowInstanceReq.getId(),
                flowInstanceReq.getTaskType().toString(), authenticationFacade.currentOrganizationId());
        variables.setAttribute(Variable.ODC_TASK_URL, odcTaskUrl);
        // set user related variables
        variables.setAttribute(Variable.USER_ID, authenticationFacade.currentUserId());
        variables.setAttribute(Variable.USER_NAME, authenticationFacade.currentUsername());
        variables.setAttribute(Variable.USER_ACCOUNT, authenticationFacade.currentUserAccountName());
        // set task related variables
        TaskType taskType = flowInstanceReq.getTaskType();
        variables.setAttribute(Variable.TASK_TYPE, taskType.getLocalizedMessage());
        variables.setAttribute(Variable.TASK_DETAILS, JsonUtils.toJson(flowInstanceReq.getParameters()));
        variables.setAttribute(Variable.TASK_DESCRIPTION, flowInstanceReq.getDescription());
        // set connection related variables
        if (Objects.nonNull(config)) {
            variables.setAttribute(Variable.CONNECTION_NAME, config.getName());
            variables.setAttribute(Variable.CONNECTION_TENANT, config.getTenantName());
            for (Entry<String, String> entry : config.getProperties().entrySet()) {
                variables.setAttribute(Variable.CONNECTION_PROPERTIES, entry.getKey(), entry.getValue());
            }
        } else {
            variables.setAttribute(Variable.CONNECTION_NAME, "");
            variables.setAttribute(Variable.CONNECTION_TENANT, "");
        }
        // set project related variables
        List<User> projectOwners = new ArrayList<>();
        List<UserResourceRole> projectUserResourceRoles = resourceRoleService.listByResourceIdAndTypeAndName(
                flowInstanceReq.getProjectId(), ResourceType.ODC_PROJECT, ResourceRoleName.OWNER.name());
        if (CollectionUtils.isNotEmpty(projectUserResourceRoles)) {
            projectOwners = userService.batchNullSafeGet(
                    projectUserResourceRoles.stream().map(UserResourceRole::getUserId).collect(Collectors.toSet()));
        }
        List<Long> projectOwnerIds = projectOwners.stream().map(User::getId).collect(Collectors.toList());
        variables.setAttribute(Variable.PROJECT_OWNER_IDS, JsonUtils.toJson(projectOwnerIds));
        List<String> projectOwnerAccounts =
                projectOwners.stream().map(User::getAccountName).collect(Collectors.toList());
        variables.setAttribute(Variable.PROJECT_OWNER_ACCOUNTS, JsonUtils.toJson(projectOwnerAccounts));
        List<String> projectOwnerNames = projectOwners.stream().map(User::getName).collect(Collectors.toList());
        variables.setAttribute(Variable.PROJECT_OWNER_NAMES, JsonUtils.toJson(projectOwnerNames));
        // set database related variables
        if (Objects.nonNull(flowInstanceReq.getDatabaseId())) {
            Database database = databaseService.detail(flowInstanceReq.getDatabaseId());
            variables.setAttribute(Variable.DATABASE_NAME, database.getName());
            if (Objects.nonNull(database.getEnvironment())) {
                String environmentNameKey = database.getEnvironment().getName();
                if (StringUtils.isTranslatable(environmentNameKey)) {
                    String environmentName = I18n.translate(StringUtils.getTranslatableKey(environmentNameKey), null,
                            LocaleContextHolder.getLocale());
                    variables.setAttribute(Variable.ENVIRONMENT_NAME, environmentName);
                }
            }
            List<User> databaseOwners = new ArrayList<>();
            List<UserResourceRole> userResourceRoles =
                    resourceRoleService.listByResourceTypeAndResourceId(ResourceType.ODC_DATABASE, database.getId());
            if (CollectionUtils.isNotEmpty(userResourceRoles)) {
                Set<Long> userIds =
                        userResourceRoles.stream().map(UserResourceRole::getUserId).collect(Collectors.toSet());
                databaseOwners = userService.batchNullSafeGet(userIds);
            } else {
                databaseOwners = projectOwners;
            }
            List<Long> ownerIds = databaseOwners.stream().map(User::getId).collect(Collectors.toList());
            variables.setAttribute(Variable.DATABASE_OWNERS_IDS, JsonUtils.toJson(ownerIds));
            List<String> ownerAccounts = databaseOwners.stream().map(User::getAccountName).collect(Collectors.toList());
            variables.setAttribute(Variable.DATABASE_OWNERS_ACCOUNTS, JsonUtils.toJson(ownerAccounts));
            List<String> ownerNames = databaseOwners.stream().map(User::getName).collect(Collectors.toList());
            variables.setAttribute(Variable.DATABASE_OWNERS_NAMES, JsonUtils.toJson(ownerNames));
        } else {
            variables.setAttribute(Variable.ENVIRONMENT_NAME, "");
            variables.setAttribute(Variable.DATABASE_NAME, "");
            variables.setAttribute(Variable.DATABASE_OWNERS_IDS, JsonUtils.toJson(Collections.emptyList()));
            variables.setAttribute(Variable.DATABASE_OWNERS_ACCOUNTS, JsonUtils.toJson(Collections.emptyList()));
            variables.setAttribute(Variable.DATABASE_OWNERS_NAMES, JsonUtils.toJson(Collections.emptyList()));
        }
        // set SQL content if task type is DatabaseChange
        if (taskType == TaskType.ASYNC) {
            DatabaseChangeParameters params = (DatabaseChangeParameters) flowInstanceReq.getParameters();
            variables.setAttribute(Variable.SQL_CONTENT, JsonUtils.toJson(params.getSqlContent()));
            if (StringUtils.isNotBlank(params.getSqlContent())) {
                List<String> splitSqlList =
                        SqlUtils.split(config.getDialectType(), params.getSqlContent(), params.getDelimiter());
                variables.setAttribute(Variable.SQL_CONTENT_JSON_ARRAY, JsonUtils.toJson(splitSqlList));
            }
        } else {
            variables.setAttribute(Variable.SQL_CONTENT, "");
            variables.setAttribute(Variable.SQL_CONTENT_JSON_ARRAY, JsonUtils.toJson(Collections.emptyList()));
        }
        // set ODC URL site
        List<Configuration> configurations = systemConfigService.queryByKeyPrefix(ODC_SITE_URL);
        if (CollectionUtils.isNotEmpty(configurations)) {
            variables.setAttribute(Variable.ODC_SITE_URL, configurations.get(0).getValue());
        }
        return variables;
    }

    private void cancelAllRelatedExternalInstance(@NonNull FlowInstance flowInstance) {
        List<FlowApprovalInstance> externalApprovalInstance = flowInstance.filterInstanceNode(inst -> {
            if (inst.getNodeType() != FlowNodeType.APPROVAL_TASK) {
                return false;
            }
            FlowApprovalInstance approvalInst = (FlowApprovalInstance) inst;
            String externalFlowInstanceId = approvalInst.getExternalFlowInstanceId();
            return !inst.getStatus().isFinalStatus() && Objects.nonNull(externalFlowInstanceId);
        }).stream().map(nodeInst -> (FlowApprovalInstance) nodeInst).collect(Collectors.toList());
        //
        HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(Collections.singleton(flowInstance.getProcessInstanceId()))
                .includeProcessVariables();
        HistoricProcessInstance processInstance = historyQuery.list().get(0);
        TemplateVariables variables = FlowTaskUtil.getTemplateVariables(processInstance.getProcessVariables());
        externalApprovalInstance.forEach(inst -> {
            try {
                IntegrationConfig config =
                        integrationService.detailWithoutPermissionCheck(inst.getExternalApprovalId());
                ApprovalProperties properties = ApprovalProperties.from(config);
                variables.setAttribute(Variable.PROCESS_INSTANCE_ID, inst.getExternalFlowInstanceId());
                approvalClient.cancel(properties, variables);
            } catch (Exception e) {
                log.warn(
                        "Failed to cancel external approval instance, flowInstanceId={}, integrationId={}, externalProcessInstanceId={}, variables={}",
                        inst.getFlowInstanceId(), inst.getExternalApprovalId(),
                        inst.getExternalFlowInstanceId(), variables, e);
            }
        });
    }

    public void addShadowTableComparingHook(Consumer<ShadowTableComparingUpdateEvent> hook) {
        shadowTableComparingTaskHooks.add(hook);
    }

    private void consumeShadowTableHook(ShadowTableSyncTaskParameter parameter, Long flowInstanceId) {
        ShadowTableComparingUpdateEvent event = new ShadowTableComparingUpdateEvent();
        event.setComparingTaskId(parameter.getComparingTaskId());
        event.setFlowInstanceId(flowInstanceId);
        for (Consumer<ShadowTableComparingUpdateEvent> hook : shadowTableComparingTaskHooks) {
            hook.accept(event);
        }
    }

    public void addDataTransferTaskInitHook(Consumer<DataTransferTaskInitEvent> hook) {
        dataTransferTaskInitHooks.add(hook);
    }

    private void consumeDataTransferHook(DataTransferConfig config, Long taskId) {
        DataTransferTaskInitEvent event = new DataTransferTaskInitEvent(taskId, config);
        for (Consumer<DataTransferTaskInitEvent> hook : dataTransferTaskInitHooks) {
            hook.accept(event);
        }
    }

    private RiskLevelDescriber buildRiskLevelDescriber(CreateFlowInstanceReq req) {
        EnvironmentEntity env = null;
        if (Objects.nonNull(req.getEnvironmentId())) {
            env = environmentRepository.findById(req.getEnvironmentId()).orElse(null);
        }
        return RiskLevelDescriber.builder()
                .projectName(req.getProjectName())
                .taskType(req.getTaskType().name())
                .environmentId(env == null ? null : String.valueOf(env.getId()))
                .environmentName(env == null ? null : env.getName())
                .databaseName(req.getDatabaseName())
                .build();
    }

    public List<FlowInstanceEntity> getFlowInstanceByParentId(Long parentFlowInstanceId) {
        return flowInstanceRepository.findByParentInstanceId(parentFlowInstanceId);
    }

    public Set<Long> getApprovingAlterScheduleById(Long parentFlowInstanceId) {
        Specification<FlowInstanceEntity> sp = Specification.where(
                FlowInstanceSpecs.parentInstanceIdIn(Collections.singletonList(parentFlowInstanceId)))
                .and(FlowInstanceSpecs.statusIn(Collections.singletonList(FlowStatus.APPROVING)));
        return flowInstanceRepository.findAll(sp).stream().map(FlowInstanceEntity::getId).collect(Collectors.toSet());
    }

    public Set<Long> listAlterScheduleIdsByScheduleIdAndStatus(Long scheduleId, FlowStatus status) {
        return flowInstanceRepository.findFlowInstanceIdByScheduleIdAndStatus(scheduleId, status);
    }

    /**
     * This is a temporary method that only uses ODC 4.3.4
     *
     * @param params
     * @return
     */
    private List<ServiceTaskInstanceEntity> innerListDistinctServiceTaskInstances(
            @NonNull InnerQueryFlowInstanceParams params) {
        StringBuilder querySql = new StringBuilder();
        HashMap<String, Object> queryParamMap = new HashMap<>();
        queryParamMap.put("organizationId", authenticationFacade.currentOrganizationId());
        querySql.append("SELECT flow_instance_id, task_type FROM flow_instance_node_task ");
        querySql.append("WHERE organization_id = :organizationId ");
        if (CollectionUtils.isNotEmpty(params.getFlowInstanceIds())) {
            queryParamMap.put("flowInstanceIds", params.getFlowInstanceIds());
            querySql.append("AND flow_instance_id in (:flowInstanceIds) ");
        }
        if (CollectionUtils.isNotEmpty(params.getTaskTypes())) {
            queryParamMap.put("taskTypes",
                    params.getTaskTypes().stream().map(Enum::name).collect(Collectors.toSet()));
            querySql.append("AND task_type in (:taskTypes) ");
        }
        if (params.getStartTime() != null) {
            queryParamMap.put("startTime", params.getStartTime());
            querySql.append("AND create_time >= :startTime ");
        }
        if (params.getEndTime() != null) {
            queryParamMap.put("endTime", params.getEndTime());
            querySql.append("AND create_time <= :endTime ");
        }
        querySql.append("GROUP BY flow_instance_id;");
        return namedParameterJdbcTemplate.query(
                querySql.toString(), queryParamMap,
                new BeanPropertyRowMapper<>(ServiceTaskInstanceEntity.class));
    }

    public int getPartitionPlanCount(@NotNull InnerQueryFlowInstanceParams params) {
        Set<Long> joinedProjectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
        if (CollectionUtils.isEmpty(joinedProjectIds)) {
            return 0;
        }
        Set<Long> partitionPlanFlowInstanceIds =
                innerListDistinctServiceTaskInstances(new InnerQueryFlowInstanceParams()
                        .setTaskTypes(Collections.singleton(TaskType.PARTITION_PLAN))
                        .setStartTime(params.getStartTime())
                        .setEndTime(params.getEndTime()))
                                .stream().map(ServiceTaskInstanceEntity::getFlowInstanceId).collect(Collectors.toSet());
        if (CollectionUtils.isNotEmpty(partitionPlanFlowInstanceIds)) {
            Specification<FlowInstanceEntity> spec = FlowInstanceSpecs
                    .organizationIdEquals(authenticationFacade.currentOrganizationId())
                    .and(FlowInstanceSpecs.idIn(partitionPlanFlowInstanceIds))
                    .and(FlowInstanceSpecs.projectIdIn(joinedProjectIds))
                    .and(FlowInstanceSpecs.statusIn(params.getFlowStatus()));
            return flowInstanceRepository.findAll(spec).size();
        }
        return 0;
    }

    /**
     * This is a temporary method that only uses ODC 4.3.4
     *
     * @param params
     * @return
     */
    private List<FlowInstanceState> listPartitionPlanSubTaskStates(@NonNull InnerQueryFlowInstanceParams params) {
        Set<Long> joinedProjectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
        if (CollectionUtils.isEmpty(joinedProjectIds) || CollectionUtils.isEmpty(params.getTaskTypes())
                || !params.getTaskTypes().contains(TaskType.PARTITION_PLAN)) {
            return Collections.emptyList();
        }
        Set<Long> partitionPlanFlowInstanceIds =
                innerListDistinctServiceTaskInstances(new InnerQueryFlowInstanceParams()
                        .setTaskTypes(Collections.singleton(TaskType.PARTITION_PLAN)))
                                .stream().map(ServiceTaskInstanceEntity::getFlowInstanceId).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(partitionPlanFlowInstanceIds)) {
            return Collections.emptyList();
        }
        Specification<FlowInstanceEntity> spec = FlowInstanceSpecs.createTimeLate(params.getStartTime())
                .and(FlowInstanceSpecs.createTimeBefore(params.getEndTime()))
                .and(FlowInstanceSpecs.parentInstanceIdIn(partitionPlanFlowInstanceIds))
                .and(FlowInstanceSpecs.projectIdIn(joinedProjectIds));
        final List<FlowInstanceState> partitionPlanFlowInstanceStates = new ArrayList<>();
        flowInstanceRepository.findAll(spec).forEach(flowInstance -> {
            partitionPlanFlowInstanceStates
                    .add(new FlowInstanceState(TaskType.PARTITION_PLAN, flowInstance.getStatus()));
        });
        return partitionPlanFlowInstanceStates;
    }

    /**
     * This is a temporary method that only uses ODC 4.3.4
     *
     * @param params
     * @return
     */
    private List<FlowInstanceState> listSqlPlanSubTaskStates(@NonNull InnerQueryFlowInstanceParams params) {
        if (CollectionUtils.isEmpty(params.getParentInstanceIds()) || CollectionUtils.isEmpty(params.getTaskTypes())) {
            return Collections.emptyList();
        }
        InnerQueryFlowInstanceParams copiedParams = ObjectUtil.deepCopy(params,
                InnerQueryFlowInstanceParams.class);
        copiedParams.getTaskTypes().retainAll(Collections.singleton(TaskType.ASYNC));
        Map<Long, FlowStatus> flowInstanceId2Status = flowInstanceRepository.findProjectionByParentInstanceIdIn(
                copiedParams.getParentInstanceIds()).stream()
                .collect(Collectors.toMap(FlowInstanceProjection::getId, FlowInstanceProjection::getStatus,
                        (exist, replace) -> exist));
        if (CollectionUtils.isEmpty(flowInstanceId2Status.keySet())) {
            return Collections.emptyList();
        }
        copiedParams.setFlowInstanceIds(flowInstanceId2Status.keySet());
        Map<Long, TaskType> flowInstanceId2SubTaskType = innerListDistinctServiceTaskInstances(copiedParams)
                .stream().collect(Collectors.toMap(ServiceTaskInstanceEntity::getFlowInstanceId,
                        ServiceTaskInstanceEntity::getTaskType, (exist, replace) -> exist));
        final List<FlowInstanceState> flowInstanceStates = new ArrayList<>();
        flowInstanceId2Status.forEach((id, flowStatus) -> {
            if (flowStatus != null && flowInstanceId2SubTaskType.containsKey(id)) {
                flowInstanceStates.add(new FlowInstanceState(TaskType.ASYNC, flowStatus));
            }
        });
        return flowInstanceStates;
    }

    /**
     * This is a temporary method that only uses ODC 4.3.4
     *
     * @param params
     * @return
     */
    public List<FlowInstanceState> listSubTaskStates(
            @NonNull InnerQueryFlowInstanceParams params) {
        Set<Long> joinedProjectIds = projectService.getMemberProjectIds(authenticationFacade.currentUserId());
        if (CollectionUtils.isEmpty(joinedProjectIds)) {
            return Collections.emptyList();
        }
        List<FlowInstanceState> flowInstanceStates = listSqlPlanSubTaskStates(params);
        flowInstanceStates.addAll(listPartitionPlanSubTaskStates(params));
        return flowInstanceStates;
    }

    @Data
    public static class ShadowTableComparingUpdateEvent {
        private Long comparingTaskId;
        private Long flowInstanceId;
    }

    @Data
    @AllArgsConstructor
    public static class FlowInstanceState {
        private TaskType taskType;
        private FlowStatus status;
    }

    @Getter
    @AllArgsConstructor
    public static class DataTransferTaskInitEvent {
        private Long taskId;
        private DataTransferConfig config;
    }

    @Data
    @Accessors(chain = true)
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class MergedDbIdentifier {
        /**
         * For a complete approval process, there are N approval nodes, and each approval node has M
         * approval User eg: [1,2] -> [1,3]
         */
        private List<Set<Long>> approvalUserIds;

        /**
         * Different databases go through the approval process corresponding to different risk levels
         */
        private Long riskLevelId;
    }

    @Data
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class MergedDbCreatedData {
        private RiskLevel riskLevel;
        private Set<Long> candidateResourceIds;
    }
}
