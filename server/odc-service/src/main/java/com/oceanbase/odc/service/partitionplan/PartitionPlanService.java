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
package com.oceanbase.odc.service.partitionplan;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.partitionplan.ConnectionPartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.ConnectionPartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanRepository;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.partitionplan.model.ConnectionPartitionPlan;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTaskParameters;
import com.oceanbase.odc.service.partitionplan.model.TablePartitionPlan;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @Author：tianke
 * @Date: 2022/9/15 15:21
 * @Descripition:
 */
@Slf4j
@Service
@SkipAuthorize("permission check inside getForConnect")
public class PartitionPlanService {

    @Autowired
    private TablePartitionPlanRepository tablePartitionPlanRepository;
    @Autowired
    private ConnectionPartitionPlanRepository connectionPartitionPlanRepository;

    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private TaskService taskService;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskInstanceRepository;


    private final ConnectionPartitionPlanMapper connectionPartitionPlanMapper = ConnectionPartitionPlanMapper.INSTANCE;

    private final TablePartitionPlanMapper tablePartitionPlanMapper = new TablePartitionPlanMapper();

    /**
     * 查找 Range 分区表 TODO:新增筛选已修改的表
     */
    public ConnectionPartitionPlan findRangeTablePlan(Long connectionId, Long flowInstanceId) {
        // 根据流程实例 ID 查询时，仅查询实例下的分区配置
        if (flowInstanceId != null) {
            return findTablePartitionPlanByFlowInstanceId(flowInstanceId);
        }
        // 通过 connectionId 查询时，拉取全 RANGE 表，并返回已存在的分区计划
        ConnectionConfig connectionConfig = connectionService.getForConnect(connectionId);
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connectionConfig);
        ConnectionSession connectionSession = factory.generateSession();
        try {
            DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
            List<TablePartitionPlan> tablePartitionPlans = findConnectionAllTablePartitionPlan(
                    connectionId, connectionConfig.getTenantName(), accessor);
            Optional<ConnectionPartitionPlanEntity> validConnectionPlan =
                    connectionPartitionPlanRepository.findValidPlanByConnectionId(connectionId);
            ConnectionPartitionPlan returnValue =
                    validConnectionPlan.isPresent()
                            ? connectionPartitionPlanMapper.entityToModel(validConnectionPlan.get())
                            : ConnectionPartitionPlan.builder().connectionId(connectionId).build();
            returnValue.setTablePartitionPlans(tablePartitionPlans);
            return returnValue;
        } finally {
            try {
                connectionSession.expire();
            } catch (Exception e) {
                // eat exception
            }
        }
    }

    /**
     * 确认策略：停用连接下原所有计划，生效当前 flowInstanceId 下的所有配置
     */
    @Transactional
    public void updateTablePartitionPlan(ConnectionPartitionPlan connectionPartitionPlan) throws IOException {
        // 更新连接配置
        Optional<ConnectionPartitionPlanEntity> connectionPartitionPlanEntity =
                connectionPartitionPlanRepository.findByFlowInstanceId(connectionPartitionPlan.getFlowInstanceId());
        if (!connectionPartitionPlanEntity.isPresent())
            return;
        connectionPartitionPlanEntity.get().setInspectEnabled(connectionPartitionPlan.isInspectEnable());
        connectionPartitionPlanEntity.get()
                .setInspectTriggerStrategy(connectionPartitionPlan.getInspectTriggerStrategy());
        connectionPartitionPlanEntity.get().setConfigEnabled(false);
        connectionPartitionPlanRepository.saveAndFlush(connectionPartitionPlanEntity.get());
        // 更新表分区配置
        List<TablePartitionPlan> tablePartitionPlans = connectionPartitionPlan.getTablePartitionPlans();
        List<TablePartitionPlanEntity> tablePartitionPlanEntities = tablePartitionPlanRepository.findByFlowInstanceId(
                connectionPartitionPlan.getFlowInstanceId());
        List<TablePartitionPlanEntity> updateEntities = new LinkedList<>();
        tablePartitionPlans.forEach(tablePartitionPlan -> {
            for (TablePartitionPlanEntity tablePartitionPlanEntity : tablePartitionPlanEntities) {
                // 未修改的直接生效
                tablePartitionPlanEntity.setFlowInstanceId(connectionPartitionPlan.getFlowInstanceId());
                tablePartitionPlanEntity.setIsConfigEnable(false);
                tablePartitionPlanEntity.setModifierId(authenticationFacade.currentUserId());
                if (tablePartitionPlanEntity.getSchemaName().equals(tablePartitionPlan.getSchemaName())
                        && tablePartitionPlanEntity.getTableName().equals(tablePartitionPlan.getTableName())) {
                    tablePartitionPlanEntity.setIsAutoPartition(tablePartitionPlan.getDetail().getIsAutoPartition());
                    tablePartitionPlanEntity.setPreCreatePartitionCount(
                            tablePartitionPlanEntity.getPreCreatePartitionCount());
                    tablePartitionPlanEntity
                            .setPartitionInterval(tablePartitionPlan.getDetail().getPartitionInterval());
                    tablePartitionPlanEntity
                            .setPartitionIntervalUnit(tablePartitionPlan.getDetail().getPartitionIntervalUnit());
                    tablePartitionPlanEntity.setExpirePeriod(tablePartitionPlan.getDetail().getExpirePeriod());
                    tablePartitionPlanEntity.setExpirePeriodUnit(tablePartitionPlan.getDetail().getExpirePeriodUnit());
                    tablePartitionPlanEntity.setPartitionNamingPrefix(
                            tablePartitionPlan.getDetail().getPartitionNamingPrefix());
                    tablePartitionPlanEntity.setPartitionNamingSuffixExpression(
                            tablePartitionPlan.getDetail().getPartitionNamingSuffixExpression());
                }
                updateEntities.add(tablePartitionPlanEntity);
            }
        });
        // 更新配置
        tablePartitionPlanRepository.saveAll(updateEntities);
        PartitionPlanTaskParameters taskParameters = new PartitionPlanTaskParameters();
        taskParameters.setConnectionPartitionPlan(connectionPartitionPlan);
        // 更新任务详情
        Optional<ServiceTaskInstanceEntity> taskInstance = serviceTaskInstanceRepository.findByFlowInstanceId(
                connectionPartitionPlan.getFlowInstanceId());
        taskInstance.ifPresent(instance -> {
            TaskEntity taskEntity = taskService.detail(instance.getTargetTaskId());
            taskEntity.setParametersJson(JsonUtils.toJson(taskParameters));
            taskService.updateParametersJson(taskEntity);
        });
        // 推进流程节点
        flowInstanceService.approve(connectionPartitionPlan.getFlowInstanceId(), "approve update partition plan",
                false);
    }

    /**
     * 查询当前连接下是否存在分区计划
     */
    public boolean hasConnectionPartitionPlan(Long connectionId) {
        Optional<ConnectionPartitionPlanEntity> validConnectionPlan =
                connectionPartitionPlanRepository.findValidPlanByConnectionId(connectionId);
        return validConnectionPlan.isPresent();
    }

    public ConnectionPartitionPlan findTablePartitionPlanByFlowInstanceId(Long flowInstanceId) {
        ConnectionPartitionPlan connectionPartitionPlan = new ConnectionPartitionPlan();
        Optional<ConnectionPartitionPlanEntity> connectionPartitionPlanEntity =
                connectionPartitionPlanRepository.findByFlowInstanceId(flowInstanceId);
        if (connectionPartitionPlanEntity.isPresent()) {
            connectionPartitionPlan =
                    connectionPartitionPlanMapper.entityToModel(connectionPartitionPlanEntity.get());
        }
        List<TablePartitionPlanEntity> tablePartitionPlanEntities =
                tablePartitionPlanRepository.findByFlowInstanceId(flowInstanceId);
        List<TablePartitionPlan> tablePartitionPlans = tablePartitionPlanEntities.stream().map(
                tablePartitionPlanMapper::entityToModel).collect(
                        Collectors.toList());
        connectionPartitionPlan.setTablePartitionPlans(tablePartitionPlans);
        return connectionPartitionPlan;
    }

    private List<TablePartitionPlan> findConnectionAllTablePartitionPlan(Long connectionId, String tenantName,
            DBSchemaAccessor accessor) {
        List<TablePartitionPlanEntity> connectionValidPartitionPlans =
                tablePartitionPlanRepository.findValidPlanByConnectionId(connectionId);
        List<DBTablePartition> dbTablePartitions = accessor.listTableRangePartitionInfo(tenantName);

        List<TablePartitionPlan> returnValue = new LinkedList<>();
        for (DBTablePartition dbTablePartition : dbTablePartitions) {
            boolean hasPartitionPlan = false;
            for (TablePartitionPlanEntity tablePlan : connectionValidPartitionPlans) {
                if (dbTablePartition.getSchemaName().equals(tablePlan.getSchemaName())
                        && dbTablePartition.getTableName().equals(tablePlan.getTableName())) {
                    TablePartitionPlan tablePartitionPlan = tablePartitionPlanMapper.entityToModel(tablePlan);
                    tablePartitionPlan.setPartitionCount(dbTablePartition.getPartitionOption().getPartitionsNum());
                    returnValue.add(tablePartitionPlan);
                    hasPartitionPlan = true;
                    break;
                }
            }
            if (!hasPartitionPlan) {
                returnValue.add(TablePartitionPlan.builder()
                        .schemaName(dbTablePartition.getSchemaName())
                        .tableName(dbTablePartition.getTableName())
                        .partitionCount(dbTablePartition.getPartitionOption().getPartitionsNum()).build());
            }
        }
        return returnValue;
    }

    /**
     * 创建分区计划时插入，审批通过后生效
     */
    @Transactional
    public void addTablePartitionPlan(ConnectionPartitionPlan connectionPartitionPlan, Long flowInstanceId) {
        connectionPartitionPlan.setFlowInstanceId(flowInstanceId);
        connectionPartitionPlan.getTablePartitionPlans()
                .forEach(tablePartitionPlan -> tablePartitionPlan.setFlowInstanceId(flowInstanceId));
        // 新增连接分区配置
        long currentUserId = authenticationFacade.currentUserId();
        long currentOrganizationId = authenticationFacade.currentOrganizationId();
        ConnectionPartitionPlanEntity connectionPartitionPlanEntity =
                connectionPartitionPlanMapper.modelToEntity(connectionPartitionPlan);
        connectionPartitionPlanEntity.setCreatorId(currentUserId);
        connectionPartitionPlanEntity.setModifierId(currentUserId);
        connectionPartitionPlanEntity.setOrganizationId(currentOrganizationId);
        connectionPartitionPlanEntity.setConfigEnabled(false);
        connectionPartitionPlanRepository.save(connectionPartitionPlanEntity);
        // 新增分区计划
        List<TablePartitionPlanEntity> entities = new LinkedList<>();
        for (TablePartitionPlan tablePlan : connectionPartitionPlan.getTablePartitionPlans()) {
            TablePartitionPlanEntity tablePlanEntity = tablePartitionPlanMapper.modelToEntity(tablePlan);
            tablePlanEntity.setConnectionId(connectionPartitionPlan.getConnectionId());
            tablePlanEntity.setFlowInstanceId(connectionPartitionPlanEntity.getFlowInstanceId());
            tablePlanEntity.setOrganizationId(currentOrganizationId);
            tablePlanEntity.setCreatorId(currentUserId);
            tablePlanEntity.setModifierId(currentUserId);
            tablePlanEntity.setIsAutoPartition(tablePlan.getDetail().getIsAutoPartition());
            tablePlanEntity.setIsConfigEnable(false);
            entities.add(tablePlanEntity);
        }
        tablePartitionPlanRepository.saveAll(entities);
    }
}
