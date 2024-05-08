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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;
import com.oceanbase.odc.metadb.flow.FlowInstanceRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanKeyConfig;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanStrategy;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanTableConfig;
import com.oceanbase.odc.service.quartz.model.MisfireStrategy;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.model.JobType;
import com.oceanbase.odc.service.schedule.model.ScheduleStatus;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link PartitionPlanScheduleService}
 *
 * @author yh263208
 * @date 2024-02-05 19:27
 * @since ODC_release_4.2.4
 */
@Slf4j
@Service
@SkipAuthorize("odc internal usage")
public class PartitionPlanScheduleService {

    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private DatabaseService databaseService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private PartitionPlanTableRepository partitionPlanTableRepository;
    @Autowired
    private PartitionPlanRepository partitionPlanRepository;
    @Autowired
    private PartitionPlanTablePartitionKeyRepository partitionPlanTablePartitionKeyRepository;
    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private FlowInstanceRepository flowInstanceRepository;

    public PartitionPlanConfig getPartitionPlanByFlowInstanceId(@NonNull Long flowInstanceId) {
        FlowInstanceDetailResp resp = this.flowInstanceService.detail(flowInstanceId);
        Optional<PartitionPlanEntity> optional = this.partitionPlanRepository.findByFlowInstanceId(resp.getId());
        if (optional.isPresent()) {
            return optional.map(this::getPartitionPlan).get();
        }
        TaskParameters parameters = resp.getParameters();
        if (!(parameters instanceof PartitionPlanConfig)) {
            return null;
        }
        PartitionPlanConfig partitionPlanConfig = (PartitionPlanConfig) parameters;
        partitionPlanConfig.setEnabled(false);
        if (CollectionUtils.isNotEmpty(partitionPlanConfig.getPartitionTableConfigs())) {
            partitionPlanConfig.getPartitionTableConfigs().forEach(t -> t.setEnabled(false));
        }
        return partitionPlanConfig;
    }

    public PartitionPlanConfig getPartitionPlanByDatabaseId(@NonNull Long databaseId) {
        Database database = this.databaseService.detail(databaseId);
        List<PartitionPlanEntity> planEntities = this.partitionPlanRepository
                .findByDatabaseIdAndEnabled(database.getId(), true);
        if (CollectionUtils.isEmpty(planEntities)) {
            return null;
        } else if (planEntities.size() > 1) {
            throw new IllegalStateException("Unknown error, there are "
                    + planEntities.size() + " partition plans are active, databaseId=" + databaseId);
        }
        return getPartitionPlan(planEntities.get(0));
    }

    public List<PartitionPlanTableConfig> getPartitionPlanTables(@NonNull List<Long> partitionPlanTableIds) {
        List<PartitionPlanTableEntity> ppts = this.partitionPlanTableRepository.findByIdIn(partitionPlanTableIds);
        if (CollectionUtils.isEmpty(ppts)) {
            return Collections.emptyList();
        }
        List<PartitionPlanTableConfig> tableConfigs = ppts.stream()
                .map(this::entityToModel).collect(Collectors.toList());
        Map<Long, List<PartitionPlanTablePartitionKeyEntity>> pptId2KeyEntities =
                this.partitionPlanTablePartitionKeyRepository.findByPartitionplanTableIdIn(tableConfigs.stream()
                        .map(PartitionPlanTableConfig::getId).collect(Collectors.toList())).stream()
                        .collect(Collectors.groupingBy(PartitionPlanTablePartitionKeyEntity::getPartitionplanTableId));
        tableConfigs.forEach(tableConfig -> {
            List<PartitionPlanTablePartitionKeyEntity> pptks = pptId2KeyEntities.get(tableConfig.getId());
            if (CollectionUtils.isEmpty(pptks)) {
                return;
            }
            tableConfig.setPartitionKeyConfigs(pptks.stream().map(this::entityToModel).collect(Collectors.toList()));
        });
        return tableConfigs;
    }

    /**
     * submit a partition plan task
     *
     * @param partitionPlanConfig config for a partition plan
     */
    @Transactional(rollbackOn = Exception.class)
    public void submit(@NonNull PartitionPlanConfig partitionPlanConfig)
            throws SchedulerException, ClassNotFoundException {
        Long databaseId = partitionPlanConfig.getDatabaseId();
        Validate.notNull(databaseId, "DatabaseId can not be null");
        // disable all related partition plan task
        Database database = this.databaseService.detail(databaseId);
        disablePartitionPlan(database.getId());
        PartitionPlanEntity partitionPlanEntity = modelToEntity(partitionPlanConfig);
        partitionPlanEntity = this.partitionPlanRepository.save(partitionPlanEntity);
        if (!partitionPlanConfig.isEnabled()
                || CollectionUtils.isEmpty(partitionPlanConfig.getPartitionTableConfigs())) {
            log.info("Partition plan is disabled or table config is empty, do nothing and return");
            return;
        }
        Validate.isTrue(partitionPlanConfig.getCreationTrigger() != null, "Creation trigger can not be null");
        if (partitionPlanConfig.getDroppingTrigger() == null) {
            ScheduleEntity createScheduleEntity = createAndEnableSchedule(
                    database, partitionPlanConfig.getCreationTrigger());
            createPartitionPlanTables(partitionPlanConfig.getPartitionTableConfigs(),
                    partitionPlanEntity.getId(), createScheduleEntity.getId(),
                    partitionPlanConfig.getFlowInstanceId(), partitionPlanConfig.getTaskId(),
                    partitionPlanConfig.getErrorStrategy(), partitionPlanConfig.getTimeoutMillis());
            return;
        }
        Map<PartitionPlanStrategy, List<PartitionPlanTableConfig>> strategy2TblCfgs =
                partitionPlanConfig.getPartitionTableConfigs().stream().flatMap(tableConfig -> {
                    Map<PartitionPlanStrategy, List<PartitionPlanKeyConfig>> strategy2Cfgs =
                            tableConfig.getPartitionKeyConfigs().stream()
                                    .collect(Collectors.groupingBy(PartitionPlanKeyConfig::getStrategy));
                    return strategy2Cfgs.values().stream().map(cfgs -> {
                        PartitionPlanTableConfig cfg = new PartitionPlanTableConfig();
                        cfg.setPartitionKeyConfigs(cfgs);
                        cfg.setTableName(tableConfig.getTableName());
                        cfg.setEnabled(tableConfig.isEnabled());
                        cfg.setPartitionNameInvoker(tableConfig.getPartitionNameInvoker());
                        cfg.setPartitionNameInvokerParameters(tableConfig.getPartitionNameInvokerParameters());
                        return cfg;
                    });
                }).collect(Collectors.groupingBy(cfg -> cfg.getPartitionKeyConfigs().get(0).getStrategy()));
        List<PartitionPlanTableConfig> createConfigs = strategy2TblCfgs.get(PartitionPlanStrategy.CREATE);
        List<PartitionPlanTableConfig> dropConfigs = strategy2TblCfgs.get(PartitionPlanStrategy.DROP);
        if (CollectionUtils.isNotEmpty(createConfigs)) {
            ScheduleEntity createScheduleEntity = createAndEnableSchedule(
                    database, partitionPlanConfig.getCreationTrigger());
            createPartitionPlanTables(createConfigs,
                    partitionPlanEntity.getId(), createScheduleEntity.getId(),
                    partitionPlanConfig.getFlowInstanceId(), partitionPlanConfig.getTaskId(),
                    partitionPlanConfig.getErrorStrategy(), partitionPlanConfig.getTimeoutMillis());
        }
        if (CollectionUtils.isNotEmpty(dropConfigs)) {
            ScheduleEntity dropScheduleEntity = createAndEnableSchedule(
                    database, partitionPlanConfig.getDroppingTrigger());
            createPartitionPlanTables(dropConfigs,
                    partitionPlanEntity.getId(), dropScheduleEntity.getId(),
                    partitionPlanConfig.getFlowInstanceId(), partitionPlanConfig.getTaskId(),
                    partitionPlanConfig.getErrorStrategy(), partitionPlanConfig.getTimeoutMillis());
        }
    }

    @Transactional(rollbackOn = Exception.class)
    public void disablePartitionPlan(@NonNull Long databaseId) throws SchedulerException {
        List<Long> ppIds = this.partitionPlanRepository.findByDatabaseIdAndEnabled(databaseId, true)
                .stream().map(PartitionPlanEntity::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ppIds)) {
            return;
        }
        List<PartitionPlanEntity> ppEntities = this.partitionPlanRepository.findByIdIn(ppIds)
                .stream().filter(e -> Boolean.TRUE.equals(e.getEnabled())).collect(Collectors.toList());
        Set<Long> flowInstIds = ppEntities.stream()
                .map(PartitionPlanEntity::getFlowInstanceId)
                .filter(id -> id > 0).collect(Collectors.toSet());
        this.flowInstanceRepository.updateStatusByIds(flowInstIds, FlowStatus.CANCELLED);
        ppIds = ppEntities.stream().map(PartitionPlanEntity::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ppIds)) {
            return;
        }
        this.partitionPlanRepository.updateEnabledAndLastModifierIdByIdIn(
                ppIds, false, this.authenticationFacade.currentUserId());
        List<Long> pptIds = this.partitionPlanTableRepository.findByPartitionPlanIdInAndEnabled(ppIds, true)
                .stream().map(PartitionPlanTableEntity::getId).collect(Collectors.toList());
        disablePartitionPlanTables(pptIds);
        log.info("Disable partition plan succeed, ids={}, databaseId={}", ppIds, databaseId);
    }

    @Transactional(rollbackOn = Exception.class)
    public void disablePartitionPlanTables(@NonNull List<Long> partitionPlanTableIds) throws SchedulerException {
        List<PartitionPlanTableEntity> ppts = this.partitionPlanTableRepository
                .findByIdIn(partitionPlanTableIds).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ppts)) {
            return;
        }
        List<Long> pptIds = ppts.stream().map(PartitionPlanTableEntity::getId).collect(Collectors.toList());
        this.partitionPlanTableRepository.updateEnabledByIdIn(pptIds, false);
        Set<Long> scheduleIds = ppts.stream().map(PartitionPlanTableEntity::getScheduleId)
                .collect(Collectors.toSet());
        for (Long scheduleId : scheduleIds) {
            scheduleService.terminate(scheduleService.nullSafeGetById(scheduleId));
        }
        log.info("Disable partition plan related tables succeed, ids={}, tableNames={}", pptIds, ppts.stream()
                .map(PartitionPlanTableEntity::getTableName).collect(Collectors.toList()));
    }

    private ScheduleEntity createAndEnableSchedule(Database database, @NonNull TriggerConfig triggerConfig)
            throws SchedulerException, ClassNotFoundException {
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setDatabaseId(database.getId());
        scheduleEntity.setStatus(ScheduleStatus.ENABLED);
        scheduleEntity.setAllowConcurrent(false);
        scheduleEntity.setMisfireStrategy(MisfireStrategy.MISFIRE_INSTRUCTION_DO_NOTHING);
        scheduleEntity.setJobType(JobType.PARTITION_PLAN);
        scheduleEntity.setTriggerConfigJson(JsonUtils.toJson(triggerConfig));
        scheduleEntity.setProjectId(database.getProject().getId());
        scheduleEntity.setConnectionId(database.getDataSource().getId());
        scheduleEntity.setDatabaseName(database.getName());
        scheduleEntity.setCreatorId(this.authenticationFacade.currentUserId());
        scheduleEntity.setModifierId(this.authenticationFacade.currentUserId());
        scheduleEntity.setOrganizationId(this.authenticationFacade.currentOrganizationId());
        scheduleEntity = this.scheduleService.create(scheduleEntity);
        this.scheduleService.enable(scheduleEntity);
        return scheduleEntity;
    }

    private PartitionPlanEntity modelToEntity(PartitionPlanConfig model) {
        PartitionPlanEntity entity = new PartitionPlanEntity();
        entity.setDatabaseId(model.getDatabaseId());
        entity.setEnabled(model.isEnabled());
        Validate.notNull(model.getFlowInstanceId(), "Flow instance id can not be null");
        entity.setFlowInstanceId(model.getFlowInstanceId());
        entity.setLastModifierId(null);
        entity.setCreatorId(this.authenticationFacade.currentUserId());
        return entity;
    }

    private PartitionPlanTableEntity modelToEntity(PartitionPlanTableConfig model,
            @NonNull Long partitionPlanId, @NonNull Long scheduleId) {
        PartitionPlanTableEntity entity = new PartitionPlanTableEntity();
        entity.setTableName(model.getTableName());
        entity.setPartitionPlanId(partitionPlanId);
        entity.setEnabled(true);
        entity.setPartitionNameInvoker(model.getPartitionNameInvoker());
        entity.setPartitionNameInvokerParameters(JsonUtils.toJson(model.getPartitionNameInvokerParameters()));
        entity.setScheduleId(scheduleId);
        return entity;
    }

    private PartitionPlanConfig entityToModel(PartitionPlanEntity entity) {
        PartitionPlanConfig target = new PartitionPlanConfig();
        target.setId(entity.getId());
        target.setEnabled(entity.getEnabled());
        target.setDatabaseId(entity.getDatabaseId());
        target.setFlowInstanceId(entity.getFlowInstanceId());
        return target;
    }

    private PartitionPlanTableConfig entityToModel(PartitionPlanTableEntity entity) {
        PartitionPlanTableConfig target = new PartitionPlanTableConfig();
        target.setId(entity.getId());
        target.setEnabled(entity.getEnabled());
        target.setTableName(entity.getTableName());
        target.setPartitionNameInvoker(entity.getPartitionNameInvoker());
        target.setPartitionNameInvokerParameters(JsonUtils.fromJson(
                entity.getPartitionNameInvokerParameters(), new TypeReference<Map<String, Object>>() {}));
        return target;
    }

    private PartitionPlanKeyConfig entityToModel(PartitionPlanTablePartitionKeyEntity entity) {
        PartitionPlanKeyConfig target = new PartitionPlanKeyConfig();
        target.setId(entity.getId());
        target.setPartitionKey(entity.getPartitionKey());
        target.setStrategy(entity.getStrategy());
        target.setPartitionKeyInvoker(entity.getPartitionKeyInvoker());
        target.setPartitionKeyInvokerParameters(JsonUtils.fromJson(
                entity.getPartitionKeyInvokerParameters(), new TypeReference<Map<String, Object>>() {}));
        return target;
    }

    private PartitionPlanTablePartitionKeyEntity modelToEntity(PartitionPlanKeyConfig model,
            @NonNull Long partitionPlanTableId) {
        PartitionPlanTablePartitionKeyEntity entity = new PartitionPlanTablePartitionKeyEntity();
        entity.setPartitionplanTableId(partitionPlanTableId);
        entity.setPartitionKey(model.getPartitionKey());
        entity.setPartitionKeyInvoker(model.getPartitionKeyInvoker());
        entity.setPartitionKeyInvokerParameters(JsonUtils.toJson(model.getPartitionKeyInvokerParameters()));
        entity.setStrategy(model.getStrategy());
        return entity;
    }

    private void createPartitionPlanTables(List<PartitionPlanTableConfig> partitionPlanTableConfigs,
            Long partitionPlanId, Long scheduleId,
            Long flowInstanceId, Long taskId, TaskErrorStrategy errorStrategy, Long timeoutMillis) {
        Validate.isTrue(CollectionUtils.isNotEmpty(partitionPlanTableConfigs),
                "Partition plan table configs can't be empty");
        List<PartitionPlanTableEntity> ppts = partitionPlanTableConfigs.stream()
                .map(t -> modelToEntity(t, partitionPlanId, scheduleId)).collect(Collectors.toList());
        Map<String, Long> tblName2Id = this.partitionPlanTableRepository.batchCreate(ppts).stream()
                .collect(Collectors.toMap(PartitionPlanTableEntity::getTableName, PartitionPlanTableEntity::getId));
        List<PartitionPlanTablePartitionKeyEntity> pptks = partitionPlanTableConfigs.stream()
                .flatMap(t -> t.getPartitionKeyConfigs().stream()
                        .map(i -> modelToEntity(i, tblName2Id.get(t.getTableName()))))
                .collect(Collectors.toList());
        this.partitionPlanTablePartitionKeyRepository.batchCreate(pptks);
        PartitionPlanConfig parameter = new PartitionPlanConfig();
        parameter.setErrorStrategy(errorStrategy);
        parameter.setTimeoutMillis(timeoutMillis);
        parameter.setId(partitionPlanId);
        parameter.setTaskId(taskId);
        parameter.setFlowInstanceId(flowInstanceId);
        parameter.setPartitionTableConfigs(ppts.stream().map(i -> {
            PartitionPlanTableConfig cfg = new PartitionPlanTableConfig();
            cfg.setId(i.getId());
            return cfg;
        }).collect(Collectors.toList()));
        this.scheduleService.updateJobParametersById(scheduleId, JsonUtils.toJson(parameter));
    }

    private PartitionPlanConfig getPartitionPlan(@NonNull PartitionPlanEntity partitionPlan) {
        PartitionPlanConfig target = entityToModel(partitionPlan);
        List<PartitionPlanTableConfig> tableConfigs = this.partitionPlanTableRepository
                .findByPartitionPlanIdIn(Collections.singletonList(partitionPlan.getId())).stream()
                .map(this::entityToModel).collect(Collectors.toList());
        target.setPartitionTableConfigs(tableConfigs);
        if (CollectionUtils.isEmpty(tableConfigs)) {
            return target;
        }
        Map<Long, List<PartitionPlanTablePartitionKeyEntity>> pptId2KeyEntities =
                this.partitionPlanTablePartitionKeyRepository.findByPartitionplanTableIdIn(tableConfigs.stream()
                        .map(PartitionPlanTableConfig::getId).collect(Collectors.toList())).stream()
                        .collect(Collectors.groupingBy(PartitionPlanTablePartitionKeyEntity::getPartitionplanTableId));
        target.getPartitionTableConfigs().forEach(tableConfig -> {
            List<PartitionPlanTablePartitionKeyEntity> pptks = pptId2KeyEntities.get(tableConfig.getId());
            if (CollectionUtils.isEmpty(pptks)) {
                return;
            }
            tableConfig.setPartitionKeyConfigs(pptks.stream().map(this::entityToModel).collect(Collectors.toList()));
        });
        return target;
    }

}


