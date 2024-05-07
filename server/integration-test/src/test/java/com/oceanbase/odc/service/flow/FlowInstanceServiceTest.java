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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.flowable.engine.FormService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.common.event.EventPublisher;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.core.shared.exception.OverLimitException;
import com.oceanbase.odc.metadb.flow.FlowInstanceEntity;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository.ParentInstanceIdCount;
import com.oceanbase.odc.metadb.flow.GateWayInstanceRepository;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceCandidateRepository;
import com.oceanbase.odc.metadb.flow.UserTaskInstanceRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.metadb.task.TaskRepository;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.flow.factory.FlowFactory;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;
import com.oceanbase.odc.service.flow.instance.FlowApprovalInstance;
import com.oceanbase.odc.service.flow.instance.FlowGatewayInstance;
import com.oceanbase.odc.service.flow.instance.FlowInstance;
import com.oceanbase.odc.service.flow.instance.FlowSequenceInstance;
import com.oceanbase.odc.service.flow.instance.FlowTaskInstance;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.ExecutionStrategyConfig;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;
import com.oceanbase.odc.service.flow.model.QueryFlowInstanceParams;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.flow.tool.TestFlowRuntimeTaskImpl;
import com.oceanbase.odc.service.flow.util.FlowInstanceUtil;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.iam.model.User;
import com.oceanbase.odc.service.permission.DBResourcePermissionHelper;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigSelector;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalNodeConfig;
import com.oceanbase.odc.service.regulation.risklevel.RiskLevelService;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.sqlcheck.SqlCheckService;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.odc.test.tool.TestRandom;

import lombok.NonNull;

/**
 * Test cases for {@link FlowInstanceService}
 *
 * @author yh263208
 * @date 2022-03-04 16:34
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
public class FlowInstanceServiceTest extends ServiceTestEnv {

    private final AtomicLong counter = new AtomicLong(1L);
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @MockBean
    private ConnectionService connectionService;
    @MockBean
    private UserService userService;
    @MockBean
    private ResourceRoleService resourceRoleService;
    @MockBean
    private TaskService taskService;
    @MockBean
    private DatabaseService databaseService;
    @MockBean
    @Qualifier("RiskLevelServiceFrom420")
    private RiskLevelService riskLevelService;
    @MockBean
    private ApprovalFlowConfigSelector approvalFlowConfigSelector;
    @MockBean
    private SqlCheckService sqlCheckService;
    @Autowired
    private org.flowable.engine.TaskService flowTaskService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private NodeInstanceEntityRepository nodeRepository;
    @Autowired
    private SequenceInstanceRepository sequenceRepository;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskRepository;
    @Autowired
    private UserTaskInstanceRepository userTaskInstanceRepository;
    @Autowired
    private GateWayInstanceRepository gateWayInstanceRepository;
    @Autowired
    private FlowableAdaptor flowableAdaptor;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private FormService formService;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private FlowFactory flowFactory;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository;
    @MockBean
    private DBResourcePermissionHelper permissionHelper;

    @Before
    public void setUp() {
        taskRepository.deleteAll();
        flowInstanceRepository.deleteAll();
        serviceTaskRepository.deleteAll();
        nodeRepository.deleteAll();
        sequenceRepository.deleteAll();
        eventPublisher.removeAllListeners();
        userTaskInstanceRepository.deleteAll();
        gateWayInstanceRepository.deleteAll();
        when(taskService.create(Mockito.any(), Mockito.anyInt())).thenReturn(createTaskEntity());
        ConnectionConfig connectionConfig = TestRandom.nextObject(ConnectionConfig.class);
        connectionConfig.setType(ConnectType.OB_MYSQL);
        Database database = TestRandom.nextObject(Database.class);
        connectionConfig.setVisibleScope(ConnectionVisibleScope.ORGANIZATION);
        when(sqlCheckService.check(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Collections.emptyList());
        when(connectionService.getForConnectionSkipPermissionCheck(Mockito.anyLong())).thenReturn(connectionConfig);
        when(databaseService.findDataSourceForConnectById(Mockito.anyLong())).thenReturn(connectionConfig);
        when(databaseService.detail(Mockito.anyLong())).thenReturn(database);
        when(riskLevelService.findDefaultRiskLevel()).thenReturn(getRiskLevel());
        when(riskLevelService.list()).thenReturn(Arrays.asList(getRiskLevel(), getRiskLevel()));
        doNothing().when(permissionHelper).checkDBPermissions(Mockito.anyCollection(), Mockito.anyCollection());
    }

    @Test
    public void create_10001exportObject_limitException() {
        // Prepare
        CreateFlowInstanceReq exportFlowInstanceReq = createExport10001ObjectsFlowInstanceReq();
        thrown.expect(OverLimitException.class);
        flowInstanceService.create(exportFlowInstanceReq);
    }

    @Test
    public void create_publicConnectionSingleRiskLevel_createSucceed() {
        when(approvalFlowConfigSelector.select(Mockito.any(RiskLevelDescriber.class)))
                .thenReturn(getRiskLevel());
        List<FlowInstanceDetailResp> resp = flowInstanceService.create(createFlowInstanceReq());
        Assert.assertFalse(resp.isEmpty());
    }

    @Test
    public void testList_NotJoinAnyProject_ContainsAll_ReturnAllTicketsOfTheProject() {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);

        flowInstance = createFlowInstance("test1");
        buildFlowInstance(flowInstance);

        QueryFlowInstanceParams params = QueryFlowInstanceParams.builder()
                .approveByCurrentUser(false)
                .createdByCurrentUser(true)
                .containsAll(true)
                .startTime(new Date(System.currentTimeMillis() - 10000))
                .endTime(new Date(System.currentTimeMillis() + 10000))
                .type(TaskType.ASYNC).build();
        Page<FlowInstanceDetailResp> page = flowInstanceService.list(Pageable.unpaged(), params);
        Assert.assertEquals(2, page.getTotalElements());
    }

    @Test
    public void testList_ProjectOwner_ContainsAll_ReturnAllTicketsOfTheProject() {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);

        flowInstance = createFlowInstance("test1");
        buildFlowInstance(flowInstance);

        when(userService.getCurrentUserResourceRoleIdentifiers()).thenReturn(Collections.singleton("1:1"));
        Map<Long, Set<ResourceRoleName>> projectId2Roles = new HashMap<>();
        projectId2Roles.put(1L, Collections.singleton(ResourceRoleName.OWNER));
        when(resourceRoleService.getProjectId2ResourceRoleNames()).thenReturn(projectId2Roles);


        QueryFlowInstanceParams params = QueryFlowInstanceParams.builder()
                .approveByCurrentUser(false)
                .createdByCurrentUser(true)
                .containsAll(true)
                .startTime(new Date(System.currentTimeMillis() - 10000))
                .endTime(new Date(System.currentTimeMillis() + 10000))
                .type(TaskType.ASYNC).build();
        Page<FlowInstanceDetailResp> page = flowInstanceService.list(Pageable.unpaged(), params);
        Assert.assertEquals(2, page.getTotalElements());
    }

    @Test
    public void list_everyParamSet_getAllFlowInstances() {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);

        flowInstance = createFlowInstance("test1");
        buildFlowInstance(flowInstance);

        User user = new User();
        user.setRoleIds(Collections.emptyList());
        when(userService.detailCurrentUser()).thenReturn(user);
        when(databaseService.listDatabasesByConnectionIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(getDatabase()));

        QueryFlowInstanceParams params = QueryFlowInstanceParams.builder()
                .approveByCurrentUser(false)
                .createdByCurrentUser(true)
                .containsAll(false)
                .startTime(new Date(System.currentTimeMillis() - 10000))
                .endTime(new Date(System.currentTimeMillis() + 10000))
                .type(TaskType.ASYNC).build();
        Page<FlowInstanceDetailResp> page = flowInstanceService.list(Pageable.unpaged(), params);
        Assert.assertEquals(2, page.getTotalElements());
    }

    @Test
    public void list_ThreePages_ReturnCorrectTotalElements() {

        for (int i = 0; i < 5; i++) {
            FlowInstance flowInstance = createFlowInstance("test" + i);
            buildFlowInstance(flowInstance);
        }

        User user = new User();
        user.setRoleIds(Collections.emptyList());
        when(userService.detailCurrentUser()).thenReturn(user);
        when(databaseService.listDatabasesByConnectionIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(getDatabase()));

        QueryFlowInstanceParams params = QueryFlowInstanceParams.builder()
                .approveByCurrentUser(false)
                .createdByCurrentUser(true)
                .containsAll(false)
                .startTime(new Date(System.currentTimeMillis() - 10000))
                .endTime(new Date(System.currentTimeMillis() + 10000))
                .type(TaskType.ASYNC).build();
        Page<FlowInstanceDetailResp> page = flowInstanceService.list(PageRequest.of(1, 2), params);
        Assert.assertEquals(5, page.getTotalElements());
        Assert.assertEquals(3, page.getTotalPages());
    }

    @Test
    public void list_by_parent_flow_instance_id_returnEmpty() {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);

        User user = new User();
        user.setRoleIds(Collections.emptyList());
        when(userService.detailCurrentUser()).thenReturn(user);
        when(databaseService.listDatabasesByConnectionIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(getDatabase()));

        QueryFlowInstanceParams params = QueryFlowInstanceParams.builder()
                .type(TaskType.ASYNC)
                .parentInstanceId(233L)
                .build();
        Page<FlowInstanceDetailResp> page = flowInstanceService.list(Pageable.unpaged(), params);
        Assert.assertEquals(0, page.getTotalElements());
    }

    @Test
    public void list_getFlowInstances_with_determined_taskType() {
        FlowInstance flowInstance = createFlowInstance("test2");
        buildFlowInstance(flowInstance);

        flowInstance = createFlowInstance("test3");
        buildFlowInstanceWithTaskType(flowInstance, TaskType.IMPORT);
        User user = new User();
        user.setRoleIds(Collections.emptyList());
        when(userService.detailCurrentUser()).thenReturn(user);
        when(databaseService.listDatabasesByConnectionIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(getDatabase()));

        QueryFlowInstanceParams params = QueryFlowInstanceParams.builder()
                .approveByCurrentUser(false)
                .createdByCurrentUser(true)
                .containsAll(false)
                .startTime(new Date(System.currentTimeMillis() - 10000))
                .endTime(new Date(System.currentTimeMillis() + 10000))
                .type(TaskType.IMPORT).build();
        Page<FlowInstanceDetailResp> page = flowInstanceService.list(Pageable.unpaged(), params);
        Assert.assertEquals(1, page.getTotalElements());
    }

    @Test
    public void detail_getDetailMessage_returnInstanceDetail() {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);
        List<BaseFlowNodeInstance> instanceList =
                flowInstance.filterInstanceNode(instance -> instance.getNodeType() == FlowNodeType.APPROVAL_TASK);
        FlowApprovalInstance approvalInstance = (FlowApprovalInstance) instanceList.get(0);
        approvalInstance.setStatus(FlowNodeStatus.EXECUTING);
        approvalInstance.update();

        User user = new User();
        user.setRoleIds(Collections.emptyList());
        when(userService.detailCurrentUser()).thenReturn(user);
        when(databaseService.listDatabasesByConnectionIds(Mockito.anyCollection()))
                .thenReturn(Collections.singletonList(getDatabase()));
        FlowInstanceDetailResp resp = flowInstanceService.detail(flowInstance.getId());
        Assert.assertEquals(flowInstance.getId(), resp.getId());
    }

    @Test
    public void detail_unauthorized_expThrown() {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);
        List<BaseFlowNodeInstance> instanceList =
                flowInstance.filterInstanceNode(instance -> instance.getNodeType() == FlowNodeType.APPROVAL_TASK);
        FlowApprovalInstance approvalInstance = (FlowApprovalInstance) instanceList.get(0);
        approvalInstance.setStatus(FlowNodeStatus.EXECUTING);
        approvalInstance.update();

        User user = new User();
        user.setRoleIds(Collections.emptyList());
        when(userService.detailCurrentUser()).thenReturn(user);

        Optional<FlowInstanceEntity> optional = flowInstanceRepository.findById(flowInstance.getId());
        Assert.assertTrue(optional.isPresent());
        FlowInstanceEntity entity = optional.get();
        entity.setCreatorId(-1);
        flowInstanceRepository.saveAndFlush(entity);

        thrown.expectMessage(String.format("ODC_FLOW_INSTANCE not found by id=%d", flowInstance.getId()));
        thrown.expect(NotFoundException.class);
        flowInstanceService.detail(flowInstance.getId());
    }

    @Test
    public void approve_unauthorizedApprove_expThrown() throws IOException {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);
        List<BaseFlowNodeInstance> instanceList =
                flowInstance.filterInstanceNode(instance -> instance.getNodeType() == FlowNodeType.APPROVAL_TASK);
        FlowApprovalInstance approvalInstance = (FlowApprovalInstance) instanceList.get(0);
        approvalInstance.setStatus(FlowNodeStatus.EXECUTING);
        approvalInstance.update();

        User user = new User();
        user.setRoleIds(Collections.emptyList());
        when(userService.detailCurrentUser()).thenReturn(user);

        when(taskService.detail(Mockito.anyLong())).thenReturn(new TaskEntity());
        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("ODC_FLOW_INSTANCE not found by id=%d", flowInstance.getId()));
        flowInstanceService.approve(flowInstance.getId(), "No Message", false);
    }

    @Test
    public void reject_unauthorizedReject_expThrown() {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);
        List<BaseFlowNodeInstance> instanceList =
                flowInstance.filterInstanceNode(instance -> instance.getNodeType() == FlowNodeType.APPROVAL_TASK);
        FlowApprovalInstance approvalInstance = (FlowApprovalInstance) instanceList.get(0);
        approvalInstance.setStatus(FlowNodeStatus.EXECUTING);
        approvalInstance.update();

        User user = new User();
        user.setRoleIds(Collections.emptyList());
        when(userService.detailCurrentUser()).thenReturn(user);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage(String.format("ODC_FLOW_INSTANCE not found by id=%d", flowInstance.getId()));
        flowInstanceService.reject(flowInstance.getId(), "No Message", false);
    }

    @Test
    public void cancelTask_taskNotRunning_cancelSucceed() {
        FlowInstance flowInstance = createFlowInstance("test");
        buildFlowInstance(flowInstance);
        List<BaseFlowNodeInstance> instanceList =
                flowInstance.filterInstanceNode(instance -> instance.getNodeType() == FlowNodeType.SERVICE_TASK);
        FlowTaskInstance taskInstance = (FlowTaskInstance) instanceList.get(0);
        taskInstance.setStatus(FlowNodeStatus.EXECUTING);
        taskInstance.update();

        Mockito.when(taskService.detail(Mockito.anyLong())).thenReturn(new TaskEntity());
        flowInstanceService.cancel(flowInstance.getId(), false);
        FlowInstanceEntity actual = flowInstanceRepository.findById(flowInstance.getId())
                .orElseThrow(IllegalStateException::new);
        Assert.assertEquals(FlowStatus.CANCELLED, actual.getStatus());
    }

    @Test
    public void listStatus() {
        FlowInstanceEntity entity = new FlowInstanceEntity();
        entity.setId(8888L);
        entity.setName("test");
        entity.setFlowConfigId(1L);
        entity.setStatus(FlowStatus.EXECUTION_SUCCEEDED);
        entity.setProcessInstanceId("1");
        entity.setFlowConfigSnapshotXml("1");
        entity.setProcessDefinitionId("1");
        entity.setCreatorId(authenticationFacade.currentUserId());
        entity.setOrganizationId(888L);
        entity.setProjectId(1L);
        flowInstanceRepository.save(entity);
        Set<Long> ids = new HashSet();
        ids.add(8888L);
        Map<Long, FlowStatus> status = flowInstanceService.getStatus(ids);
        Assert.assertEquals(0, status.size());
    }

    @Test
    public void testFindByParentInstanceIdIn() {
        createChildFlowInstance("test", 1L);
        List<ParentInstanceIdCount> byParentInstanceIdIn = flowInstanceRepository.findByParentInstanceIdIn(
                Arrays.asList(1L, 2L));
        Assert.assertEquals(byParentInstanceIdIn.size(), 1);
    }

    private void buildFlowInstance(FlowInstance flowInstance) {
        buildFlowInstanceWithTaskType(flowInstance, TaskType.ASYNC);
    }

    private void buildFlowInstanceWithTaskType(FlowInstance flowInstance, TaskType taskType) {
        TaskEntity taskEntity = TestRandom.nextObject(TaskEntity.class);
        taskEntity.setId(null);
        taskEntity.setTaskType(taskType);
        taskRepository.save(taskEntity);

        FlowGatewayInstance gatewayInstance = createGatewayInstance(flowInstance.getId(), true, false);
        FlowApprovalInstance first_route_approval_1 = createApprovalInstance(flowInstance.getId());
        FlowGatewayInstance first_route_approval_1_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowApprovalInstance first_route_approval_2 = createApprovalInstance(flowInstance.getId());
        FlowGatewayInstance first_route_approval_2_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowTaskInstance first_route_task_1 = createTaskInstance(flowInstance.getId(), taskEntity,
                ExecutionStrategyConfig.autoStrategy());
        FlowApprovalInstance second_route_approval_1 = createApprovalInstance(flowInstance.getId());
        FlowGatewayInstance second_route_approval_1_gateway = createGatewayInstance(flowInstance.getId(), false, true);
        FlowTaskInstance second_route_task_1 = createTaskInstance(flowInstance.getId(), taskEntity,
                ExecutionStrategyConfig.manualStrategy(20, TimeUnit.SECONDS));
        Set<FlowSequenceInstance> sequences = new HashSet<>();
        sequences.add(new FlowSequenceInstance(gatewayInstance, first_route_approval_1));
        sequences.add(new FlowSequenceInstance(first_route_approval_1, first_route_approval_1_gateway));
        sequences.add(new FlowSequenceInstance(first_route_approval_1_gateway, first_route_approval_2));
        sequences.add(new FlowSequenceInstance(first_route_approval_2, first_route_approval_2_gateway));
        sequences.add(new FlowSequenceInstance(first_route_approval_2_gateway, first_route_task_1));
        sequences.add(new FlowSequenceInstance(gatewayInstance, second_route_approval_1));
        sequences.add(new FlowSequenceInstance(second_route_approval_1, second_route_approval_1_gateway));
        sequences.add(new FlowSequenceInstance(second_route_approval_1_gateway, second_route_task_1));
        FlowInstanceUtil.loadTopology(flowInstance,
                Arrays.asList(gatewayInstance, first_route_approval_1, first_route_approval_1_gateway,
                        first_route_approval_2, first_route_approval_2_gateway, first_route_task_1,
                        second_route_approval_1, second_route_approval_1_gateway, second_route_task_1),
                sequences);
        flowInstance.forEachInstanceNode(inst -> inst.setId(null));
        flowInstance.buildTopology();
    }

    private FlowInstance createFlowInstance(String name) {
        return flowFactory.generateFlowInstance(name, null, 1L, null);
    }

    private FlowInstance createChildFlowInstance(String name, Long parentFloweInstanceId) {
        return flowFactory.generateFlowInstance(name, parentFloweInstanceId, 1L, null);
    }

    private FlowTaskInstance createTaskInstance(Long flowInstanceId, TaskEntity taskEntity,
            ExecutionStrategyConfig config) {
        FlowTaskInstance taskInstance = new FlowTaskInstance(taskEntity.getTaskType(),
                authenticationFacade.currentOrganizationId(), flowInstanceId, config, false, true,
                type -> TestFlowRuntimeTaskImpl.class, flowableAdaptor, eventPublisher, flowTaskService,
                nodeRepository, sequenceRepository, serviceTaskRepository);
        taskInstance.setId(counter.incrementAndGet());
        taskInstance.setTargetTaskId(taskEntity.getId());
        return taskInstance;
    }

    private FlowApprovalInstance createApprovalInstance(Long flowInstanceId) {
        FlowApprovalInstance inst = new TestFlowApprovalInstance(authenticationFacade.currentOrganizationId(),
                flowInstanceId, 10, false, false, flowableAdaptor, flowTaskService, formService, eventPublisher,
                authenticationFacade, nodeRepository, sequenceRepository,
                userTaskInstanceRepository, userTaskInstanceCandidateRepository);
        inst.setId(counter.incrementAndGet());
        return inst;
    }

    private FlowGatewayInstance createGatewayInstance(Long flowInstanceId, boolean startEndPoint, boolean endEndPoint) {
        FlowGatewayInstance inst = new FlowGatewayInstance(authenticationFacade.currentOrganizationId(),
                flowInstanceId, startEndPoint, endEndPoint, flowableAdaptor, nodeRepository, sequenceRepository,
                gateWayInstanceRepository);
        inst.setId(counter.incrementAndGet());
        return inst;
    }

    private CreateFlowInstanceReq createFlowInstanceReq() {
        CreateFlowInstanceReq req = TestRandom.nextObject(CreateFlowInstanceReq.class);
        req.setProjectId(1L);
        req.setTaskType(TaskType.ASYNC);
        DatabaseChangeParameters asyncParam = new DatabaseChangeParameters();
        asyncParam.setErrorStrategy(TaskErrorStrategy.ABORT.name());
        asyncParam.setSqlContent("select 1 from dual");
        asyncParam.setRollbackSqlContent("select 1 from dual");
        req.setParameters(asyncParam);
        return req;
    }

    private CreateFlowInstanceReq createExport10001ObjectsFlowInstanceReq() {
        CreateFlowInstanceReq req = TestRandom.nextObject(CreateFlowInstanceReq.class);
        req.setTaskType(TaskType.EXPORT);
        DataTransferConfig param = new DataTransferConfig();
        param.setExportDbObjects(
                IntStream.range(0, 10001).mapToObj(i -> new DataTransferObject(null, null))
                        .collect(Collectors.toList()));
        req.setParameters(param);
        return req;
    }

    private TaskEntity createTaskEntity() {
        TaskEntity taskEntity = TestRandom.nextObject(TaskEntity.class);
        taskEntity.setTaskType(TaskType.EXPORT);
        DatabaseChangeParameters asyncParam = new DatabaseChangeParameters();
        asyncParam.setErrorStrategy(TaskErrorStrategy.ABORT.name());
        asyncParam.setRollbackSqlContent("select 1 from dual");
        taskEntity.setParametersJson(JsonUtils.toJson(asyncParam));
        return taskEntity;
    }

    private RiskLevel getRiskLevel() {
        RiskLevel riskLevel = new RiskLevel();
        riskLevel.setId(1L);
        riskLevel.setLevel(0);
        riskLevel.setApprovalFlowConfigId(1L);
        riskLevel.setApprovalFlowConfig(getApprovalFlowConfig());
        return riskLevel;
    }

    private ApprovalFlowConfig getApprovalFlowConfig() {
        ApprovalFlowConfig config = new ApprovalFlowConfig();
        config.setId(1L);
        config.setName("fake_config");
        config.setApprovalExpirationIntervalSeconds(1);
        config.setExecutionExpirationIntervalSeconds(1);
        config.setWaitExecutionExpirationIntervalSeconds(1);
        config.setNodes(getApprovalNodes());
        return config;
    }

    private Database getDatabase() {
        Database database = new Database();
        database.setId(1L);
        ConnectionConfig connectionConfig = new ConnectionConfig();
        connectionConfig.setId(1L);
        database.setDataSource(connectionConfig);
        return database;
    }

    private List<ApprovalNodeConfig> getApprovalNodes() {
        List<ApprovalNodeConfig> nodes = new ArrayList<>();
        ApprovalNodeConfig first = new ApprovalNodeConfig();
        first.setExternalApproval(false);
        first.setResourceRoleId(1L);
        first.setAutoApproval(false);

        ApprovalNodeConfig next = new ApprovalNodeConfig();
        next.setExternalApproval(false);
        next.setResourceRoleId(2L);
        next.setAutoApproval(false);

        ApprovalNodeConfig last = new ApprovalNodeConfig();
        last.setExternalApproval(false);
        last.setResourceRoleId(3L);
        last.setAutoApproval(true);

        nodes.add(first);
        nodes.add(next);
        nodes.add(last);

        return nodes;
    }

}


class TestFlowApprovalInstance extends FlowApprovalInstance {

    public TestFlowApprovalInstance(@NonNull Long organizationId, @NonNull Long flowInstanceId,
            @NonNull Integer expireIntervalSeconds, boolean startEndpoint, boolean endEndPoint,
            @NonNull FlowableAdaptor flowableAdaptor, @NonNull org.flowable.engine.TaskService taskService,
            @NonNull FormService formService,
            @NonNull EventPublisher eventPublisher,
            @NonNull AuthenticationFacade authenticationFacade,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository,
            @NonNull UserTaskInstanceRepository userTaskInstanceRepository,
            @NonNull UserTaskInstanceCandidateRepository userTaskInstanceCandidateRepository) {
        super(organizationId, flowInstanceId, null, expireIntervalSeconds, startEndpoint, endEndPoint, false,
                flowableAdaptor, taskService, formService, eventPublisher, authenticationFacade, nodeRepository,
                sequenceRepository, userTaskInstanceRepository, userTaskInstanceCandidateRepository);
    }

    @Override
    public void approve(String comment, boolean requireOperator) {

    }

    @Override
    public void disApprove(String comment, boolean requireOperator) {

    }

    @Override
    public boolean isPresentOnThisMachine() {
        return true;
    }

}
