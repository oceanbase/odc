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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.lang.Holder;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.LimitMetric;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.core.shared.exception.VerifyException;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceSpecs;
import com.oceanbase.odc.metadb.flow.FlowInstanceViewEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceViewRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceViewSpecs;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceSpecs;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SqlUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
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
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp.FlowInstanceMapper;
import com.oceanbase.odc.service.flow.model.FlowMetaInfo;
import com.oceanbase.odc.service.flow.model.FlowNodeInstanceDetailResp.FlowNodeInstanceMapper;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.model.QueryFlowInstanceParams;
import com.oceanbase.odc.service.flow.model.TaskParameters;
import com.oceanbase.odc.service.flow.processor.EnablePreprocess;
import com.oceanbase.odc.service.flow.task.BaseRuntimeFlowableDelegate;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.RuntimeTaskConstants;
import com.oceanbase.odc.service.flow.task.model.ShadowTableSyncTaskParameter;
import com.oceanbase.odc.service.flow.util.FlowTaskUtil;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.client.ApprovalClient;
import com.oceanbase.odc.service.integration.model.ApprovalProperties;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.integration.model.TemplateVariables;
import com.oceanbase.odc.service.integration.model.TemplateVariables.Variable;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalNodeConfig;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.flowtask.AlterScheduleParameters;
import com.oceanbase.odc.service.schedule.flowtask.OperationType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.service.task.model.ExecutorInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
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
    private ScheduleService scheduleService;
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

    private final List<Consumer<DataTransferTaskInitEvent>> dataTransferTaskInitHooks = new ArrayList<>();
    private final List<Consumer<ShadowTableComparingUpdateEvent>> shadowTableComparingTaskHooks = new ArrayList<>();
    private static final long MAX_EXPORT_OBJECT_COUNT = 10000;
    private static final String ODC_SITE_URL = "odc.site.url";

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
        return Collections.singletonList(buildWithoutApprovalNode(createReq, conn));
    }

    @EnablePreprocess
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    public List<FlowInstanceDetailResp> create(@NotNull @Valid CreateFlowInstanceReq createReq) {
        // TODO 原终止逻辑想表达的语意是终止执行中的计划，但目前线上的语意是终止审批流。暂保留逻辑，待前端修改后删除。
        if (createReq.getTaskType() == TaskType.ALTER_SCHEDULE) {
            AlterScheduleParameters parameters = (AlterScheduleParameters) createReq.getParameters();
            if (parameters.getOperationType() == OperationType.TERMINATION) {
                ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(parameters.getTaskId());
                if (scheduleEntity.getStatus() == ScheduleStatus.APPROVING) {
                    Set<Long> flowInstanceIds = getApprovingAlterScheduleById(scheduleEntity.getId());
                    return flowInstanceIds.stream().map(id -> cancel(id, false))
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
        }
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
        ConnectionConfig conn = connectionService.getForConnectionSkipPermissionCheck(createReq.getConnectionId());
        return Collections.singletonList(buildFlowInstance(riskLevels, createReq, conn));
    }

    public Page<FlowInstanceDetailResp> list(@NotNull Pageable pageable, @NotNull QueryFlowInstanceParams params) {
        Page<FlowInstanceEntity> returnValue = listAll(pageable, params);
        if (returnValue.isEmpty()) {
            return Page.empty();
        }
        FlowInstanceMapper mapper = mapperFactory.generateMapperByEntities(returnValue.getContent());
        return returnValue.map(mapper::map);
    }

    public FlowMetaInfo getMetaInfo() {
        List<UserTaskInstanceEntity> entities = approvalPermissionService.getApprovableApprovalInstances().stream()
                .filter(entity -> entity.getStatus() == FlowNodeStatus.EXECUTING).collect(Collectors.toList());
        return FlowMetaInfo.of(entities);
    }

    public Page<FlowInstanceEntity> listAll(@NotNull Pageable pageable, @NotNull QueryFlowInstanceParams params) {
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
            // TODO Remove the checker after the SQL console development is completed
            if (params.getProjectId() != null) {
                specification = specification.and(FlowInstanceSpecs.projectIdEquals(params.getProjectId()));
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
                .and(FlowInstanceViewSpecs.taskTypeEquals(params.getType()))
                .and(FlowInstanceViewSpecs.projectIdEquals(params.getProjectId()))
                .and(FlowInstanceViewSpecs.statusIn(params.getStatuses()))
                .and(FlowInstanceViewSpecs.createTimeLate(params.getStartTime()))
                .and(FlowInstanceViewSpecs.createTimeBefore(params.getEndTime()))
                .and(FlowInstanceViewSpecs.idEquals(targetId));

        Set<String> resourceRoleIdentifiers = userService.getCurrentUserResourceRoleIdentifiers();
        if (params.getApproveByCurrentUser() && params.getCreatedByCurrentUser()) {
            if (CollectionUtils.isEmpty(resourceRoleIdentifiers)) {
                specification =
                        specification.and(FlowInstanceViewSpecs.creatorIdEquals(authenticationFacade.currentUserId()));
                return flowInstanceViewRepository.findAll(specification, pageable).map(FlowInstanceEntity::from);
            }
            specification =
                    specification.and(FlowInstanceViewSpecs.leftJoinFlowInstanceApprovalView(
                            resourceRoleIdentifiers, authenticationFacade.currentUserId()));
            return flowInstanceViewRepository.findAll(specification, pageable).map(FlowInstanceEntity::from);
        } else if (!params.getApproveByCurrentUser() && params.getCreatedByCurrentUser()) {
            // 我发起的
            specification =
                    specification.and(FlowInstanceViewSpecs.creatorIdEquals(authenticationFacade.currentUserId()));
            return flowInstanceViewRepository.findAll(specification, pageable).map(FlowInstanceEntity::from);
        } else if (params.getApproveByCurrentUser() && !params.getCreatedByCurrentUser()) {
            // 待我审批
            if (CollectionUtils.isEmpty(resourceRoleIdentifiers)) {
                return Page.empty();
            }
            specification =
                    specification.and(FlowInstanceViewSpecs.leftJoinFlowInstanceApprovalView(
                            resourceRoleIdentifiers, null));
            return flowInstanceViewRepository.findAll(specification, pageable).map(FlowInstanceEntity::from);
        } else {
            throw new UnsupportedOperationException("Unsupported list flow instance query");
        }
    }

    public List<FlowInstanceEntity> listByTaskTypeAndApproveUserId(TaskType taskType,
            Long organizationId, Long approveUserId) {
        return listByTaskTypeAndApproveUserId(taskType, organizationId, approveUserId,
                entity -> entity.getStatus() == FlowNodeStatus.EXECUTING);
    }

    public List<FlowInstanceEntity> listByIds(@NonNull Collection<Long> ids) {
        return flowInstanceRepository.findByIdIn(ids);
    }

    public List<FlowInstanceEntity> listByTaskTypeAndApproveUserId(TaskType taskType,
            Long organizationId, Long approveUserId, Predicate<? super UserTaskInstanceEntity> predicate) {
        Set<Long> flowInstanceIds = new HashSet<>();
        if (Objects.nonNull(approveUserId)) {
            Set<Long> approvableFlowInstanceIds =
                    approvalPermissionService.getApprovableApprovalInstances(approveUserId)
                            .stream().filter(predicate)
                            .map(UserTaskInstanceEntity::getFlowInstanceId).collect(Collectors.toSet());
            flowInstanceIds.addAll(approvableFlowInstanceIds);
        }
        if (flowInstanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        if (Objects.nonNull(taskType)) {
            Specification<ServiceTaskInstanceEntity> instanceSpecification =
                    Specification.where(ServiceTaskInstanceSpecs.taskTypeEquals(taskType))
                            .and(ServiceTaskInstanceSpecs.flowInstanceIdIn(flowInstanceIds));
            List<Long> targetFlowInstanceIds = serviceTaskRepository.findAll(instanceSpecification).stream()
                    .map(ServiceTaskInstanceEntity::getFlowInstanceId).collect(Collectors.toList());
            flowInstanceIds.retainAll(targetFlowInstanceIds);
        }
        if (flowInstanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        Specification<FlowInstanceEntity> specification = Specification
                .where(FlowInstanceSpecs.idIn(flowInstanceIds))
                .and(FlowInstanceSpecs.organizationIdEquals(organizationId));
        return flowInstanceRepository.findAll(specification);
    }

    public FlowInstanceDetailResp detail(@NotNull Long id) {
        return mapFlowInstance(id, flowInstance -> {
            FlowInstanceMapper instanceMapper = mapperFactory.generateMapperByInstance(flowInstance);
            FlowNodeInstanceMapper nodeInstanceMapper = mapperFactory.generateNodeMapperByInstance(flowInstance);
            return instanceMapper.map(flowInstance, nodeInstanceMapper);
        }, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public FlowInstanceDetailResp cancel(@NotNull Long id, Boolean skipAuth) {
        FlowInstance flowInstance = mapFlowInstance(id, flowInst -> flowInst, skipAuth);
        if (!skipAuth) {
            long userId = authenticationFacade.currentUserId();
            if (!Objects.equals(flowInstance.getCreatorId(), userId)) {
                throw new VerifyException("The current user is not creator.");
            }
        }
        scheduleService.updateStatusByFlowInstanceId(id, ScheduleStatus.TERMINATION);
        return cancel(flowInstance, skipAuth);
    }

    public FlowInstanceDetailResp cancelNotCheckPermission(@NotNull Long id) {
        FlowInstance flowInstance = mapFlowInstance(id, flowInst -> flowInst, false);
        return cancel(flowInstance, false);
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
                throw new IllegalStateException("RollBack task can not be cancelled");
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
        log.info("Flow status error, cancellation conditions are not met, forced cancellation, flowInstanceId={}, "
                + "nodes={}", id,
                instances.stream().map(t -> t.getId() + "," + t.getNodeType() + "," + t.getStatus())
                        .collect(Collectors.toList()));
        flowInstanceRepository.updateStatusById(id, FlowStatus.CANCELLED);
        return FlowInstanceDetailResp.withIdAndType(id, taskTypeHolder.getValue());
    }

    public FlowInstanceDetailResp approve(@NotNull Long id, String message, Boolean skipAuth) throws IOException {
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
        completeApprovalInstance(id, instance -> instance.approve(message, !skipAuth), skipAuth);
        return FlowInstanceDetailResp.withIdAndType(id, taskEntity.getTaskType());
    }

    @Transactional(rollbackFor = Exception.class)
    public FlowInstanceDetailResp reject(@NotNull Long id, String message, Boolean skipAuth) {
        completeApprovalInstance(id, instance -> {
            instance.disApprove(message, !skipAuth);
            flowInstanceRepository.updateStatusById(instance.getFlowInstanceId(), FlowStatus.REJECTED);

        }, skipAuth);
        scheduleService.updateStatusByFlowInstanceId(id, ScheduleStatus.REJECTED);
        Optional<FlowInstance> optional = flowFactory.getFlowInstance(id);
        FlowInstance flowInstance =
                optional.orElseThrow(() -> new NotFoundException(ResourceType.ODC_FLOW_INSTANCE, "id", id));
        cancelAllRelatedExternalInstance(flowInstance);
        return FlowInstanceDetailResp.withIdAndType(id, getTaskByFlowInstanceId(id).getTaskType());
    }

    public <T> T mapFlowInstance(@NonNull Long flowInstanceId, Function<FlowInstance, T> function, Boolean skipAuth) {
        Optional<FlowInstance> optional = flowFactory.getFlowInstance(flowInstanceId);
        FlowInstance flowInstance =
                optional.orElseThrow(() -> new NotFoundException(ResourceType.ODC_FLOW_INSTANCE, "id", flowInstanceId));
        try {
            if (!skipAuth) {
                if (!Objects.equals(authenticationFacade.currentUserId(), flowInstance.getCreatorId())) {
                    List<UserTaskInstanceEntity> entities = approvalPermissionService.getApprovableApprovalInstances();
                    Set<Long> flowInstanceIds = entities.stream().map(UserTaskInstanceEntity::getFlowInstanceId)
                            .collect(Collectors.toSet());
                    PreConditions.validExists(ResourceType.ODC_FLOW_INSTANCE, "id", flowInstanceId,
                            () -> flowInstanceIds.contains(flowInstanceId));
                }
                permissionValidator.checkCurrentOrganization(flowInstance);
            }
            return function.apply(flowInstance);
        } finally {
            flowInstance.dealloc();
        }
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
                flowInstanceReq.getProjectId(),
                flowInstanceReq.getDescription());
        Verify.notNull(flowInstance.getId(), "FlowInstance id can not be null");
        ExecutionStrategyConfig strategyConfig = ExecutionStrategyConfig.from(flowInstanceReq,
                ExecutionStrategyConfig.INVALID_EXPIRE_INTERVAL_SECOND);
        try {
            FlowTaskInstance taskInstance =
                    flowFactory.generateFlowTaskInstance(flowInstance.getId(), true, true, taskType,
                            strategyConfig);
            taskInstance.setTargetTaskId(taskEntity.getId());
            taskInstance.update();
            TaskParameters parameters = flowInstanceReq.getParameters();
            FlowInstanceConfigurer taskConfigurer;
            if (taskType == TaskType.ASYNC
                    && Boolean.TRUE.equals(((DatabaseChangeParameters) parameters).getGenerateRollbackPlan())) {
                FlowTaskInstance rollbackPlanInstance =
                        flowFactory.generateFlowTaskInstance(flowInstance.getId(), false, false,
                                TaskType.GENERATE_ROLLBACK, ExecutionStrategyConfig.autoStrategy());
                taskConfigurer = flowInstance.newFlowInstance().next(rollbackPlanInstance).next(taskInstance);
            } else {
                taskConfigurer = flowInstance.newFlowInstance().next(taskInstance);
            }
            taskConfigurer.endFlowInstance();
            flowInstance.buildTopology();

            Map<String, Object> variables = new HashMap<>();
            FlowTaskUtil.setTemplateVariables(variables, buildTemplateVariables(flowInstanceReq, connectionConfig));
            initVariables(variables, taskEntity, null, connectionConfig, buildRiskLevelDescriber(flowInstanceReq));
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
            CreateFlowInstanceReq flowInstanceReq, ConnectionConfig connectionConfig) {
        log.info("Start creating flow instance, flowInstanceReq={}", flowInstanceReq);
        CreateFlowInstanceReq preCheckReq = new CreateFlowInstanceReq();
        preCheckReq.setConnectionId(flowInstanceReq.getConnectionId());
        preCheckReq.setTaskType(TaskType.PRE_CHECK);
        preCheckReq.setConnectionId(flowInstanceReq.getConnectionId());
        preCheckReq.setDatabaseId(flowInstanceReq.getDatabaseId());
        TaskEntity preCheckTaskEntity = taskService.create(preCheckReq, (int) TimeUnit.SECONDS
                .convert(flowTaskProperties.getDefaultExecutionExpirationIntervalHours(), TimeUnit.HOURS));

        TaskEntity taskEntity = taskService.create(flowInstanceReq, (int) TimeUnit.SECONDS
                .convert(flowTaskProperties.getDefaultExecutionExpirationIntervalHours(), TimeUnit.HOURS));
        Verify.notNull(taskEntity.getId(), "TaskId can not be null");
        FlowInstance flowInstance = flowFactory.generateFlowInstance(generateFlowInstanceName(flowInstanceReq),
                flowInstanceReq.getParentFlowInstanceId(),
                flowInstanceReq.getProjectId(),
                flowInstanceReq.getDescription());
        Verify.notNull(flowInstance.getId(), "FlowInstance id can not be null");

        try {
            FlowTaskInstance riskDetectInstance = flowFactory.generateFlowTaskInstance(flowInstance.getId(), true,
                    false, TaskType.PRE_CHECK,
                    ExecutionStrategyConfig.autoStrategy());
            riskDetectInstance.setTargetTaskId(preCheckTaskEntity.getId());
            riskDetectInstance.update();
            FlowGatewayInstance riskLevelGateway =
                    flowFactory.generateFlowGatewayInstance(flowInstance.getId(), false, true);
            FlowInstanceConfigurer startConfigurer =
                    flowInstance.newFlowInstance().next(riskDetectInstance).next(riskLevelGateway);
            for (int i = 0; i < riskLevels.size(); i++) {
                FlowInstanceConfigurer targetConfigurer = buildConfigurer(riskLevels.get(i).getApprovalFlowConfig(),
                        flowInstance, flowInstanceReq.getTaskType(), taskEntity.getId(),
                        flowInstanceReq.getParameters(), flowInstanceReq);
                startConfigurer.route(
                        String.format("${%s == %d}", RuntimeTaskConstants.RISKLEVEL, riskLevels.get(i).getLevel()),
                        targetConfigurer);
            }
            flowInstance.buildTopology();
        } catch (Exception e) {
            log.warn("Failed to build FlowInstance, flowInstanceReq={}", flowInstanceReq, e);
            throw e;
        } finally {
            flowInstance.dealloc();
        }
        Map<String, Object> variables = new HashMap<>();
        FlowTaskUtil.setFlowInstanceId(variables, flowInstance.getId());
        FlowTaskUtil.setTemplateVariables(variables, buildTemplateVariables(flowInstanceReq, connectionConfig));
        initVariables(variables, taskEntity, preCheckTaskEntity, connectionConfig,
                buildRiskLevelDescriber(flowInstanceReq));
        flowInstance.start(variables);
        if (flowInstanceReq.getTaskType() == TaskType.SHADOWTABLE_SYNC) {
            consumeShadowTableHook((ShadowTableSyncTaskParameter) flowInstanceReq.getParameters(),
                    flowInstance.getId());
        } else if (flowInstanceReq.getTaskType() == TaskType.EXPORT) {
            consumeDataTransferHook((DataTransferConfig) flowInstanceReq.getParameters(), taskEntity.getId());
        }
        log.info("New flow instance succeeded, instanceId={}, flowInstanceReq={}",
                flowInstance.getId(), flowInstanceReq);
        return FlowInstanceDetailResp.withIdAndType(flowInstance.getId(), flowInstanceReq.getTaskType());
    }

    private String generateFlowInstanceName(@NonNull CreateFlowInstanceReq req) {
        String schemaName = req.getDatabaseName();
        String connectionName = req.getConnectionId() == null ? "no_connection" : req.getConnectionId() + "";
        if (schemaName == null) {
            schemaName = "no_schema";
        }
        return schemaName + "_" + connectionName;
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
        for (int nodeSequence = 0; nodeSequence < nodeConfigs.size(); nodeSequence++) {
            FlowInstanceConfigurer configurer;
            ApprovalNodeConfig nodeConfig = nodeConfigs.get(nodeSequence);
            Long resourceRoleId = nodeConfig.getResourceRoleId();
            FlowApprovalInstance approvalInstance = flowFactory.generateFlowApprovalInstance(flowInstance.getId(),
                    false, false,
                    nodeConfig.getAutoApproval(), approvalFlowConfig.getApprovalExpirationIntervalSeconds(),
                    nodeConfig.getExternalApprovalId());
            if (Objects.nonNull(resourceRoleId)) {
                approvalPermissionService.setCandidateResourceRole(approvalInstance.getId(),
                        StringUtils.join(flowInstanceReq.getProjectId(), ":", resourceRoleId));
            }
            FlowGatewayInstance approvalGatewayInstance =
                    flowFactory.generateFlowGatewayInstance(flowInstance.getId(), false, true);
            configurer = flowInstance.newFlowInstanceConfigurer(approvalInstance);
            configurer = configurer.next(approvalGatewayInstance).route(String.format("${!%s}",
                    FlowApprovalInstance.APPROVAL_VARIABLE_NAME), flowInstance.endFlowInstance());
            if (nodeSequence == nodeConfigs.size() - 1) {
                ExecutionStrategyConfig strategyConfig = ExecutionStrategyConfig.from(flowInstanceReq,
                        approvalFlowConfig.getWaitExecutionExpirationIntervalSeconds());
                FlowTaskInstance taskInstance = flowFactory.generateFlowTaskInstance(flowInstance.getId(), false, true,
                        taskType, strategyConfig);
                taskInstance.setTargetTaskId(targetTaskId);
                taskInstance.update();
                FlowInstanceConfigurer taskConfigurer;
                if (taskType == TaskType.ASYNC
                        && Boolean.TRUE.equals(((DatabaseChangeParameters) parameters).getGenerateRollbackPlan())) {
                    FlowTaskInstance rollbackPlanInstance =
                            flowFactory.generateFlowTaskInstance(flowInstance.getId(), false, false,
                                    TaskType.GENERATE_ROLLBACK, ExecutionStrategyConfig.autoStrategy());
                    taskConfigurer = flowInstance.newFlowInstanceConfigurer(rollbackPlanInstance).next(taskInstance);
                } else {
                    taskConfigurer = flowInstance.newFlowInstanceConfigurer(taskInstance);
                }
                taskConfigurer.endFlowInstance();
                configurer.route(String.format("${%s}", FlowApprovalInstance.APPROVAL_VARIABLE_NAME),
                        taskConfigurer);
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
                mapFlowInstance(flowInstanceId, flowInstance -> flowInstance.filterInstanceNode(instance -> {
                    if (instance.getNodeType() != FlowNodeType.APPROVAL_TASK) {
                        return false;
                    }
                    return instance.getStatus() == FlowNodeStatus.EXECUTING
                            || instance.getStatus() == FlowNodeStatus.WAIT_FOR_CONFIRM;
                }).stream().map(instance -> {
                    Verify.verify(instance instanceof FlowApprovalInstance, "FlowApprovalInstance's type is illegal");
                    return (FlowApprovalInstance) instance;
                }).collect(Collectors.toList()), skipAuth);
        PreConditions.validExists(ResourceType.ODC_FLOW_APPROVAL_INSTANCE,
                "flowInstanceId", flowInstanceId, () -> instances.size() > 0);
        Verify.singleton(instances, "ApprovalInstance");
        FlowApprovalInstance target = instances.get(0);
        if (!skipAuth) {
            PreConditions.validExists(ResourceType.ODC_FLOW_INSTANCE, "id", flowInstanceId,
                    () -> approvalPermissionService.isApprovable(target.getId()));
        }
        Verify.verify(target.isPresentOnThisMachine(), "Approval instance is not on this machine");
        consumer.accept(target);
    }

    private void initVariables(Map<String, Object> variables, TaskEntity taskEntity, TaskEntity preCheckTaskEntity,
            ConnectionConfig config,
            RiskLevelDescriber riskLevelDescriber) {
        FlowTaskUtil.setTaskId(variables, taskEntity.getId());
        if (Objects.nonNull(preCheckTaskEntity)) {
            FlowTaskUtil.setPreCheckTaskId(variables, preCheckTaskEntity.getId());
        }
        if (config != null) {
            FlowTaskUtil.setConnectionConfig(variables, config);
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
        FlowTaskUtil.setTaskSubmitter(variables, JsonUtils.fromJson(taskEntity.getSubmitter(), ExecutorInfo.class));
        FlowTaskUtil.setRiskLevelDescriber(variables, riskLevelDescriber);
        FlowTaskUtil.setCloudMainAccountId(variables, authenticationFacade.currentUser().getUid());
    }

    private TemplateVariables buildTemplateVariables(CreateFlowInstanceReq flowInstanceReq, ConnectionConfig config) {
        TemplateVariables variables = new TemplateVariables();
        // set user related variables
        variables.setAttribute(Variable.USER_ID, authenticationFacade.currentUserId());
        variables.setAttribute(Variable.USER_NAME, authenticationFacade.currentUsername());
        variables.setAttribute(Variable.USER_ACCOUNT, authenticationFacade.currentUserAccountName());
        // set task related variables
        TaskType taskType = flowInstanceReq.getTaskType();
        variables.setAttribute(Variable.TASK_TYPE, taskType.getLocalizedMessage());
        variables.setAttribute(Variable.TASK_DETAILS, JsonUtils.toJson(flowInstanceReq.getParameters()));
        // set connection related variables
        if (Objects.nonNull(config)) {
            variables.setAttribute(Variable.CONNECTION_NAME, config.getName());
            variables.setAttribute(Variable.CONNECTION_TENANT, config.getTenantName());
            for (Entry<String, String> entry : config.getProperties().entrySet()) {
                variables.setAttribute(Variable.CONNECTION_PROPERTIES, entry.getKey(), entry.getValue());
            }
        }
        // set SQL content if task type is DatabaseChange
        if (taskType == TaskType.ASYNC) {
            DatabaseChangeParameters params = (DatabaseChangeParameters) flowInstanceReq.getParameters();
            String sqlContent = params.getSqlContent();
            variables.setAttribute(Variable.SQL_CONTENT, sqlContent);
            if (StringUtils.isNotBlank(sqlContent)) {
                List<String> splitSqlList = SqlUtils.split(config.getDialectType(), sqlContent, params.getDelimiter());
                variables.setAttribute(Variable.SQL_CONTENT_JSON_ARRAY, JsonUtils.toJson(splitSqlList));
            }
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
        return RiskLevelDescriber.builder()
                .projectName(req.getProjectName())
                .taskType(req.getTaskType().name())
                .environmentId(String.valueOf(req.getEnvironmentId()))
                .databaseName(req.getDatabaseName())
                .build();
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

    @Data
    public static class ShadowTableComparingUpdateEvent {
        private Long comparingTaskId;
        private Long flowInstanceId;
    }

    @Getter
    @AllArgsConstructor
    public static class DataTransferTaskInitEvent {
        private Long taskId;
        private DataTransferConfig config;
    }
}
