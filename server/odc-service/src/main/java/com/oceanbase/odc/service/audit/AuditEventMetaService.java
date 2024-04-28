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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.collect.Lists;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.AuditEventAction;
import com.oceanbase.odc.core.shared.constant.AuditEventType;
import com.oceanbase.odc.metadb.audit.AuditEventMetaEntity;
import com.oceanbase.odc.metadb.audit.AuditEventMetaRepository;
import com.oceanbase.odc.metadb.audit.AuditSpecs;
import com.oceanbase.odc.service.audit.model.AuditEventMeta;
import com.oceanbase.odc.service.audit.model.QueryAuditEventMetaParams;
import com.oceanbase.odc.service.audit.util.AuditEventMetaMapper;
import com.oceanbase.odc.service.audit.util.AuditUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/1/24 上午11:12
 * @Description: []
 */
@Slf4j
@Validated
@Service
@SkipAuthorize("public readonly metadata")
public class AuditEventMetaService {

    @Autowired
    private AuditEventHandler auditEventHandler;
    @Autowired
    private AuditEventMetaRepository auditEventMetaRepository;

    private AuditEventMetaMapper mapper = AuditEventMetaMapper.INSTANCE;

    public List<AuditEventMeta> listAllAuditEventMeta(@NotNull QueryAuditEventMetaParams params,
            @NotNull Pageable pageable) {
        Specification<AuditEventMetaEntity> specification = AuditSpecs.of(params);
        Page<AuditEventMetaEntity> auditEventMetasInDb = auditEventMetaRepository
                .findAll(specification, pageable);
        List<AuditEventMetaEntity> actualEventMetas = Lists.newArrayList();
        for (AuditEventMetaEntity entity : auditEventMetasInDb) {
            if (AuditEventType.UNKNOWN_TASK_TYPE == entity.getType()) {
                actualEventMetas.addAll(AuditUtils.createEntitiesByTypes(entity, Arrays.asList(AuditEventType.MOCKDATA,
                        AuditEventType.ASYNC,
                        AuditEventType.IMPORT,
                        AuditEventType.EXPORT,
                        AuditEventType.EXPORT_RESULT_SET,
                        AuditEventType.STRUCTURE_COMPARISON,
                        AuditEventType.PERMISSION_APPLY,
                        AuditEventType.SHADOWTABLE_SYNC,
                        AuditEventType.PARTITION_PLAN,
                        AuditEventType.ALTER_SCHEDULE,
                        AuditEventType.APPLY_PROJECT_PERMISSION,
                        AuditEventType.APPLY_DATABASE_PERMISSION,
                        AuditEventType.APPLY_TABLE_PERMISSION)));
                continue;
            }
            if (AuditEventAction.OTHERS == entity.getAction()) {
                actualEventMetas.addAll(AuditUtils.createEntitiesByActions(entity,
                        Arrays.asList(AuditEventAction.SELECT,
                                AuditEventAction.DELETE,
                                AuditEventAction.INSERT,
                                AuditEventAction.REPLACE,
                                AuditEventAction.UPDATE,
                                AuditEventAction.SET,
                                AuditEventAction.DROP,
                                AuditEventAction.ALTER,
                                AuditEventAction.TRUNCATE,
                                AuditEventAction.CREATE,
                                AuditEventAction.OTHERS)));
                continue;
            }
            if (AuditEventAction.ENABLE_USER == entity.getAction()) {
                actualEventMetas
                        .addAll(AuditUtils.createEntitiesByActions(entity, Arrays.asList(AuditEventAction.ENABLE_USER,
                                AuditEventAction.DISABLE_USER)));
                continue;
            }
            if (AuditEventAction.ENABLE_ROLE == entity.getAction()) {
                actualEventMetas
                        .addAll(AuditUtils.createEntitiesByActions(entity, Arrays.asList(AuditEventAction.ENABLE_ROLE,
                                AuditEventAction.DISABLE_ROLE)));
                continue;
            }
            if (AuditEventAction.ENABLE_RESOURCE_GROUP == entity.getAction()) {
                actualEventMetas.addAll(
                        AuditUtils.createEntitiesByActions(entity, Arrays.asList(AuditEventAction.ENABLE_RESOURCE_GROUP,
                                AuditEventAction.DISABLE_RESOURCE_GROUP)));
            }
            if (AuditEventAction.ENABLE_CONNECTION == entity.getAction()) {
                actualEventMetas.addAll(
                        AuditUtils.createEntitiesByActions(entity, Arrays.asList(AuditEventAction.ENABLE_CONNECTION,
                                AuditEventAction.DISABLE_CONNECTION)));
                continue;
            }
            if (AuditEventAction.ENABLE_FLOW_CONFIG == entity.getAction()) {
                actualEventMetas.addAll(
                        AuditUtils.createEntitiesByActions(entity, Arrays.asList(AuditEventAction.ENABLE_FLOW_CONFIG,
                                AuditEventAction.DISABLE_FLOW_CONFIG)));
                continue;
            }
            if (AuditEventAction.ENABLE_DATA_MASKING_RULE == entity.getAction()) {
                actualEventMetas.addAll(
                        AuditUtils.createEntitiesByActions(entity,
                                Arrays.asList(AuditEventAction.ENABLE_DATA_MASKING_RULE,
                                        AuditEventAction.DISABLE_DATA_MASKING_RULE)));
                continue;
            }
            actualEventMetas.add(entity);
        }
        // 去重
        actualEventMetas = actualEventMetas.stream()
                .map(meta -> {
                    meta.setAction(AuditUtils.getActualActionForTask(meta.getType(), meta.getAction()));
                    return meta;
                })
                .filter(meta -> meta.getAction() != AuditEventAction.STOP_TASK
                        && meta.getAction() != AuditEventAction.EXECUTE_TASK)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(
                                Comparator.comparing(
                                        AuditEventMetaEntity::getAction))),
                        ArrayList::new));
        // 桌面版需要过滤掉一些审计事件
        return this.auditEventHandler.filterAuditEventMeta(actualEventMetas.stream()
                .map(entity -> mapper.entityToModel(entity)).collect(Collectors.toList()));
    }


    public Optional<AuditEventMeta> findAuditEventMetaByMethodSignature(String methodSignature) {
        return auditEventMetaRepository.findByMethodSignature(methodSignature)
                .map(entity -> mapper.entityToModel(entity));
    }

    public Optional<AuditEventMeta> findAuditEventMetaByMethodSignatureIfEnabled(String methodSignature) {
        Optional<AuditEventMeta> optional = auditEventMetaRepository.findByMethodSignature(methodSignature)
                .map(entity -> mapper.entityToModel(entity));
        if (optional.isPresent()) {
            return optional.get().getEnabled() ? Optional.of(optional.get()) : Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * for support unit testing
     */
    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("for unit testing only")
    public void deleteAllAuditEventMeta() {
        auditEventMetaRepository.deleteAll();
        log.info("Delete all records in audit_event_meta successfully");
    }

    /**
     * for support unit testing
     */
    @Transactional(rollbackFor = Exception.class)
    @SkipAuthorize("for unit testing only")
    public void saveAndFlush(AuditEventMeta auditEventMeta) {
        auditEventMetaRepository.saveAndFlush(mapper.modelToEntity(auditEventMeta));
        log.info("Save audit event meta successfully, type={}, action={}, methodSignature={}",
                auditEventMeta.getType().name(),
                auditEventMeta.getAction().name(), auditEventMeta.getMethodSignature());
    }
}
