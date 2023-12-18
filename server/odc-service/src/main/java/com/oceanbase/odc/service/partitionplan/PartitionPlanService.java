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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceEntity;
import com.oceanbase.odc.metadb.flow.ServiceTaskInstanceRepository;
import com.oceanbase.odc.metadb.partitionplan.DatabasePartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.DatabasePartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.TablePartitionPlanRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.partitionplan.model.DatabasePartitionPlan;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTaskParameters;
import com.oceanbase.odc.service.partitionplan.model.TablePartitionPlan;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.PartitionPlanJobParameters;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.TaskService;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;
import com.oceanbase.tools.migrator.common.exception.UnExpectedException;

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

    @Value("${odc.task.partition-plan.schedule-cron:0 0 * * * ?}")
    private String defaultScheduleCron;
    @Autowired
    private TablePartitionPlanRepository tablePartitionPlanRepository;
    @Autowired
    private DatabasePartitionPlanRepository databasePartitionPlanRepository;

    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private TaskService taskService;
    @Autowired
    private ServiceTaskInstanceRepository serviceTaskInstanceRepository;


    private final DatabasePartitionPlanMapper databasePartitionPlanMapper = DatabasePartitionPlanMapper.INSTANCE;

    private final TablePartitionPlanMapper tablePartitionPlanMapper = new TablePartitionPlanMapper();

    /**
     * 查找 Range 分区表
     */
    public DatabasePartitionPlan findRangeTablePlan(Long databaseId, Long flowInstanceId) {
        // 根据流程实例 ID 查询时，仅查询实例下的分区配置
        if (flowInstanceId != null) {
            return findDatabasePartitionPlanByFlowInstanceId(flowInstanceId);
        }
        // 通过 database 查询时，拉取全 RANGE 表，并返回已存在的分区计划
        Database database = databaseService.detail(databaseId);
        Optional<DatabasePartitionPlanEntity> databasePartitionPlan =
                databasePartitionPlanRepository.findValidPlanByDatabaseId(databaseId);
        DatabasePartitionPlan returnValue =
                databasePartitionPlan.isPresent()
                        ? databasePartitionPlanMapper.entityToModel(databasePartitionPlan.get())
                        : DatabasePartitionPlan.builder().databaseId(databaseId).build();
        returnValue.setTablePartitionPlans(findDatabaseTablePartitionPlan(database));
        return returnValue;
    }

    /**
     * 确认策略：停用连接下原所有计划，生效当前 flowInstanceId 下的所有配置
     */
    @Transactional
    public void updateTablePartitionPlan(DatabasePartitionPlan databasePartitionPlan) throws IOException {
        // 更新连接配置
        Optional<DatabasePartitionPlanEntity> databasePartitionPlanEntity =
                databasePartitionPlanRepository.findByFlowInstanceId(databasePartitionPlan.getFlowInstanceId());
        if (!databasePartitionPlanEntity.isPresent())
            return;
        databasePartitionPlanEntity.get().setInspectEnabled(databasePartitionPlan.isInspectEnable());
        databasePartitionPlanEntity.get()
                .setInspectTriggerStrategy(databasePartitionPlan.getInspectTriggerStrategy());
        databasePartitionPlanEntity.get().setConfigEnabled(false);
        databasePartitionPlanRepository.saveAndFlush(databasePartitionPlanEntity.get());
        // 更新表分区配置
        List<TablePartitionPlan> tablePartitionPlans = databasePartitionPlan.getTablePartitionPlans();
        List<TablePartitionPlanEntity> tablePartitionPlanEntities =
                tablePartitionPlanRepository.findValidPlanByDatabasePartitionPlanId(
                        databasePartitionPlan.getId());
        List<TablePartitionPlanEntity> updateEntities = new LinkedList<>();
        tablePartitionPlans.forEach(tablePartitionPlan -> {
            for (TablePartitionPlanEntity tablePartitionPlanEntity : tablePartitionPlanEntities) {
                // 未修改的直接生效
                tablePartitionPlanEntity.setFlowInstanceId(databasePartitionPlan.getFlowInstanceId());
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
        taskParameters.setConnectionPartitionPlan(databasePartitionPlan);
        // 更新任务详情
        Optional<ServiceTaskInstanceEntity> taskInstance = serviceTaskInstanceRepository.findByFlowInstanceId(
                databasePartitionPlan.getFlowInstanceId());
        taskInstance.ifPresent(instance -> {
            TaskEntity taskEntity = taskService.detail(instance.getTargetTaskId());
            taskEntity.setParametersJson(JsonUtils.toJson(taskParameters));
            taskService.updateParametersJson(taskEntity);
        });
        // 推进流程节点
        flowInstanceService.approve(databasePartitionPlan.getFlowInstanceId(), "approve update partition plan",
                false);
    }

    /**
     * 查询当前连接下是否存在分区计划
     */
    public boolean hasConnectionPartitionPlan(Long databaseId) {
        databaseService.detail(databaseId);
        Optional<DatabasePartitionPlanEntity> validConnectionPlan =
                databasePartitionPlanRepository.findValidPlanByDatabaseId(databaseId);
        return validConnectionPlan.isPresent();
    }

    public DatabasePartitionPlan findDatabasePartitionPlanByFlowInstanceId(Long flowInstanceId) {
        Optional<DatabasePartitionPlanEntity> entity =
                databasePartitionPlanRepository.findByFlowInstanceId(flowInstanceId);
        DatabasePartitionPlan databasePartitionPlan = new DatabasePartitionPlan();
        if (entity.isPresent()) {
            databasePartitionPlan = databasePartitionPlanMapper.entityToModel(entity.get());
            List<TablePartitionPlan> tablePartitionPlans = tablePartitionPlanRepository
                    .findValidPlanByDatabasePartitionPlanId(
                            databasePartitionPlan.getId())
                    .stream().map(tablePartitionPlanMapper::entityToModel).collect(Collectors.toList());
            databasePartitionPlan.setTablePartitionPlans(tablePartitionPlans);
        }
        return databasePartitionPlan;
    }

    private List<TablePartitionPlan> findDatabaseTablePartitionPlan(Database database) {
        // find exist table partition-plan config.
        Map<String, TablePartitionPlanEntity> tableName2TablePartitionPlan = tablePartitionPlanRepository
                .findValidPlanByDatabaseId(
                        database.getId())
                .stream().collect(Collectors.toMap(TablePartitionPlanEntity::getTableName, o -> o));

        List<DBTablePartition> dbTablePartitions = listTableRangePartitionInfo(database);

        List<TablePartitionPlan> returnValue = new LinkedList<>();
        dbTablePartitions.forEach(dbTablePartition -> {
            TablePartitionPlan tablePartitionPlan =
                    tableName2TablePartitionPlan.containsKey(dbTablePartition.getTableName())
                            ? tablePartitionPlanMapper
                                    .entityToModel(tableName2TablePartitionPlan.get(dbTablePartition.getTableName()))
                            : TablePartitionPlan.builder()
                                    .schemaName(dbTablePartition.getSchemaName())
                                    .tableName(dbTablePartition.getTableName()).build();
            tablePartitionPlan.setPartitionCount(dbTablePartition.getPartitionOption().getPartitionsNum());
            returnValue.add(tablePartitionPlan);

        });
        return returnValue;
    }

    private List<DBTablePartition> listTableRangePartitionInfo(Database database) {
        ConnectionConfig connectionConfig = database.getDataSource();
        DefaultConnectSessionFactory factory = new DefaultConnectSessionFactory(connectionConfig);
        ConnectionSession connectionSession = factory.generateSession();
        DBSchemaAccessor accessor = DBSchemaAccessors.create(connectionSession);
        List<DBTablePartition> dbTablePartitions;
        try {
            dbTablePartitions = accessor.listTableRangePartitionInfo(
                    database.getDataSource().getTenantName()).stream()
                    .filter(o -> o.getSchemaName().equals(database.getName())).collect(
                            Collectors.toList());
        } finally {
            try {
                connectionSession.expire();
            } catch (Exception e) {
                // eat exception
            }
        }
        return dbTablePartitions;
    }

    @Transactional
    public void createDatabasePartitionPlan(DatabasePartitionPlan databasePartitionPlan) {
        // 新增连接分区配置
        long currentUserId = authenticationFacade.currentUserId();
        long currentOrganizationId = authenticationFacade.currentOrganizationId();
        DatabasePartitionPlanEntity databasePartitionPlanEntity =
                databasePartitionPlanMapper.modelToEntity(databasePartitionPlan);
        databasePartitionPlanEntity.setCreatorId(currentUserId);
        databasePartitionPlanEntity.setModifierId(currentUserId);
        databasePartitionPlanEntity.setOrganizationId(currentOrganizationId);
        databasePartitionPlanEntity.setConfigEnabled(false);
        databasePartitionPlanEntity = databasePartitionPlanRepository.save(databasePartitionPlanEntity);
        // 新增分区计划
        List<TablePartitionPlanEntity> entities = new LinkedList<>();
        for (TablePartitionPlan tablePlan : databasePartitionPlan.getTablePartitionPlans()) {
            TablePartitionPlanEntity tablePlanEntity = tablePartitionPlanMapper.modelToEntity(tablePlan);
            tablePlanEntity.setFlowInstanceId(databasePartitionPlanEntity.getFlowInstanceId());
            tablePlanEntity.setConnectionId(databasePartitionPlan.getConnectionId());
            tablePlanEntity.setDatabaseId(databasePartitionPlan.getDatabaseId());
            tablePlanEntity.setDatabasePartitionPlanId(databasePartitionPlanEntity.getId());
            tablePlanEntity.setOrganizationId(currentOrganizationId);
            tablePlanEntity.setCreatorId(currentUserId);
            tablePlanEntity.setModifierId(currentUserId);
            tablePlanEntity.setIsAutoPartition(tablePlan.getDetail().getIsAutoPartition());
            tablePlanEntity.setIsConfigEnable(false);
            entities.add(tablePlanEntity);
        }
        tablePartitionPlanRepository.saveAll(entities);
        enableDatabasePartitionPlan(databasePartitionPlanEntity);
        try {
            // Create partition plan job.
            ScheduleEntity scheduleEntity = createDatabasePartitionPlanSchedule(
                    databasePartitionPlanEntity.getDatabaseId(), databasePartitionPlanEntity.getId(),
                    databasePartitionPlan.getTriggerConfig());
            // Bind partition plan job to entity.
            databasePartitionPlanRepository.updateScheduleIdById(databasePartitionPlanEntity.getId(),
                    scheduleEntity.getId());
        } catch (Exception e) {
            throw new UnExpectedException("Create database partition plan job failed.", e);
        }
    }

    private void enableDatabasePartitionPlan(DatabasePartitionPlanEntity databasePartitionPlan) {
        Optional<DatabasePartitionPlanEntity> oldPartitionPlan =
                databasePartitionPlanRepository.findValidPlanByDatabaseId(databasePartitionPlan.getDatabaseId());
        if (oldPartitionPlan.isPresent()) {
            try {
                log.info("Found a valid plan in this database,start to disable it.");
                // Cancel previous plan.
                flowInstanceService.cancelNotCheckPermission(oldPartitionPlan.get().getFlowInstanceId());
                // Stop previous job.
                if (oldPartitionPlan.get().getScheduleId() != null) {
                    try {
                        ScheduleEntity scheduleEntity = scheduleService.nullSafeGetById(
                                oldPartitionPlan.get().getScheduleId());
                        scheduleService.terminate(scheduleEntity);
                        log.info("Terminate old partition plan job success,scheduleId={}",
                                oldPartitionPlan.get().getScheduleId());
                    } catch (Exception e) {
                        log.warn("Terminate old partition plan job failed,scheduleId={}",
                                oldPartitionPlan.get().getScheduleId());
                    }
                }
            } catch (Exception e) {
                log.warn("The previous plan has been abandoned,but stop previous instance failed.", e);
            }
        }
        databasePartitionPlanRepository.disableConfigByDatabaseId(databasePartitionPlan.getDatabaseId());
        tablePartitionPlanRepository.disableConfigByDatabaseId(databasePartitionPlan.getDatabaseId());
        databasePartitionPlanRepository.enableConfigByFlowInstanceId(databasePartitionPlan.getFlowInstanceId());
        tablePartitionPlanRepository.enableConfigByDatabasePartitionPlanId(databasePartitionPlan.getId());
    }

    /**
     * Create a quartz job to execute partition-plan.
     */
    @Transactional
    public ScheduleEntity createDatabasePartitionPlanSchedule(Long databaseId, Long partitionPlanId,
            TriggerConfig triggerConfig)
            throws SchedulerException, ClassNotFoundException {
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setDatabaseId(databaseId);
        scheduleEntity.setStatus(ScheduleStatus.ENABLED);
        scheduleEntity.setCreatorId(authenticationFacade.currentUserId());
        scheduleEntity.setModifierId(authenticationFacade.currentUserId());
        scheduleEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
        scheduleEntity.setAllowConcurrent(false);
        scheduleEntity.setMisfireStrategy(MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING);
        scheduleEntity.setJobType(JobType.PARTITION_PLAN);

        scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(triggerConfig));

        Database database = databaseService.detail(scheduleEntity.getDatabaseId());
        scheduleEntity.setProjectId(database.getProject().getId());
        scheduleEntity.setConnectionId(database.getDataSource().getId());
        scheduleEntity.setDatabaseName(database.getName());
        PartitionPlanJobParameters jobParameters = new PartitionPlanJobParameters();
        jobParameters.setDatabasePartitionPlanId(partitionPlanId);
        scheduleEntity.setJobParametersJson(JsonUtils.toJson(jobParameters));
        scheduleEntity = scheduleService.create(scheduleEntity);
        scheduleService.enable(scheduleEntity);
        return scheduleEntity;
    }

    public DatabasePartitionPlanEntity getDatabasePartitionPlanById(Long id) {
        return databasePartitionPlanRepository.findById(id).orElse(null);
    }

    public List<TablePartitionPlanEntity> getValidTablePlanByDatabasePartitionPlanId(Long databaseId) {
        return tablePartitionPlanRepository.findValidPlanByDatabasePartitionPlanId(databaseId);
    }
}
