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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyEntity;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTablePartitionKeyRepository;
import com.oceanbase.odc.metadb.partitionplan.PartitionPlanTableRepository;
import com.oceanbase.odc.metadb.schedule.ScheduleEntity;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
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
        Validate.isTrue(partitionPlanConfig.getCreationTrigger() != null, "Creation trigger can not be null");
        ScheduleEntity createScheduleEntity = createAndEnableSchedule(
                database, partitionPlanConfig.getCreationTrigger());
        ScheduleEntity dropScheduleEntity = null;
        if (partitionPlanConfig.getDroppingTrigger() != null) {
            dropScheduleEntity = createAndEnableSchedule(database, partitionPlanConfig.getDroppingTrigger());
        }
        if (dropScheduleEntity == null) {
            createPartitionPlanTables(partitionPlanConfig.getPartitionTableConfigs(),
                    partitionPlanEntity.getId(), createScheduleEntity.getId(),
                    partitionPlanConfig.getMaxErrors(), partitionPlanConfig.getTimeoutMillis());
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
                        cfg.setPartitionType(tableConfig.getPartitionType());
                        cfg.setTableName(tableConfig.getTableName());
                        cfg.setEnabled(tableConfig.isEnabled());
                        cfg.setPartitionNameInvoker(tableConfig.getPartitionNameInvoker());
                        cfg.setPartitionNameInvokerParameters(tableConfig.getPartitionNameInvokerParameters());
                        return cfg;
                    });
                }).collect(Collectors.groupingBy(cfg -> cfg.getPartitionKeyConfigs().get(0).getStrategy()));
        createPartitionPlanTables(strategy2TblCfgs.get(PartitionPlanStrategy.CREATE),
                partitionPlanEntity.getId(), createScheduleEntity.getId(),
                partitionPlanConfig.getMaxErrors(), partitionPlanConfig.getTimeoutMillis());
        createPartitionPlanTables(strategy2TblCfgs.get(PartitionPlanStrategy.DROP),
                partitionPlanEntity.getId(), dropScheduleEntity.getId(),
                partitionPlanConfig.getMaxErrors(), partitionPlanConfig.getTimeoutMillis());
    }

    @Transactional(rollbackOn = Exception.class)
    public void disablePartitionPlan(@NonNull Long databaseId) throws SchedulerException {
        List<Long> ppIds = this.partitionPlanRepository.findByDatabaseIdAndEnabled(databaseId, true)
                .stream().map(PartitionPlanEntity::getId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(ppIds)) {
            return;
        }
        ppIds = this.partitionPlanRepository.findByIdIn(ppIds).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled()))
                .map(PartitionPlanEntity::getId).collect(Collectors.toList());
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
        List<Long> pptkIds = this.partitionPlanTablePartitionKeyRepository
                .findByPartitionplanTableIdInAndEnabled(pptIds, true).stream()
                .map(PartitionPlanTablePartitionKeyEntity::getId).collect(Collectors.toList());
        disablePartitionPlanPartitionKeys(pptkIds);
    }

    @Transactional(rollbackOn = Exception.class)
    public void disablePartitionPlanPartitionKeys(@NonNull List<Long> partitionPlanTableKeyIds) {
        List<PartitionPlanTablePartitionKeyEntity> pptks = this.partitionPlanTablePartitionKeyRepository
                .findByIdIn(partitionPlanTableKeyIds).stream()
                .filter(e -> Boolean.TRUE.equals(e.getEnabled())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pptks)) {
            return;
        }
        List<Long> pptkIds = pptks.stream().map(PartitionPlanTablePartitionKeyEntity::getId)
                .collect(Collectors.toList());
        this.partitionPlanTablePartitionKeyRepository.updateEnabledByIdIn(pptkIds, false);
        log.info("Disable partition plan related partition keys succeed, ids={}, pkNames={}", pptkIds, pptks.stream()
                .map(PartitionPlanTablePartitionKeyEntity::getPartitionKey).collect(Collectors.toList()));
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
        entity.setEnabled(true);
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

    private PartitionPlanTablePartitionKeyEntity modelToEntity(PartitionPlanKeyConfig model,
            @NonNull Long partitionPlanTableId) {
        PartitionPlanTablePartitionKeyEntity entity = new PartitionPlanTablePartitionKeyEntity();
        entity.setPartitionplanTableId(partitionPlanTableId);
        entity.setEnabled(true);
        entity.setPartitionKey(model.getPartitionKey());
        entity.setPartitionKeyInvoker(model.getPartitionKeyInvoker());
        entity.setPartitionKeyInvokerParameters(JsonUtils.toJson(model.getPartitionKeyInvokerParameters()));
        entity.setStrategy(model.getStrategy());
        return entity;
    }

    private void createPartitionPlanTables(List<PartitionPlanTableConfig> partitionPlanTableConfigs,
            Long partitionPlanId, Long scheduleId, Integer maxErrors, Long timeoutMillis) {
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
        parameter.setMaxErrors(maxErrors);
        parameter.setTimeoutMillis(timeoutMillis);
        parameter.setId(partitionPlanId);
        parameter.setPartitionTableConfigs(ppts.stream().map(i -> {
            PartitionPlanTableConfig cfg = new PartitionPlanTableConfig();
            cfg.setId(i.getId());
            return cfg;
        }).collect(Collectors.toList()));
        this.scheduleService.updateJobParametersById(scheduleId, JsonUtils.toJson(parameter));
    }

}


