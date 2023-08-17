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
package com.oceanbase.odc.service.regulation.approval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.constraints.NotBlank;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.resourcerole.ResourceRoleEntity;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalFlowConfigEntity;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalFlowConfigRepository;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalNodeConfigEntity;
import com.oceanbase.odc.metadb.regulation.approval.ApprovalNodeConfigRepository;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelEntity;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelRepository;
import com.oceanbase.odc.service.iam.ResourceRoleService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.integration.model.IntegrationConfig;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalFlowConfig;
import com.oceanbase.odc.service.regulation.approval.model.ApprovalNodeConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/6/14 22:09
 * @Description: []
 */
@Service
@Validated
@Authenticated
public class ApprovalFlowConfigService {
    private final ApprovalFlowConfigMapper approvalFlowConfigMapper = ApprovalFlowConfigMapper.INSTANCE;

    private final ApprovalFlowNodeConfigMapper approvalFlowNodeConfigMapper = ApprovalFlowNodeConfigMapper.INSTANCE;

    private final List<Consumer<ApprovalFlowConfigDeleteEvent>> preDeleteHooks = new ArrayList<>();

    @Autowired
    private ApprovalFlowConfigRepository approvalFlowConfigRepository;

    @Autowired
    private ApprovalNodeConfigRepository nodeConfigRepository;

    @Autowired
    private ResourceRoleService resourceRoleService;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Autowired
    private RiskLevelRepository riskLevelRepository;

    @SkipAuthorize("internal authenticated")
    public List<ApprovalFlowConfig> list() {
        List<ApprovalFlowConfigEntity> flowConfigEntities =
                listByOrganizationId(authenticationFacade.currentOrganizationId());
        Map<Long, List<ApprovalNodeConfigEntity>> flowId2Nodes = nodeConfigRepository
                .findByApprovalFlowConfigIdIn(flowConfigEntities.stream().map(ApprovalFlowConfigEntity::getId).collect(
                        Collectors.toSet()))
                .stream()
                .collect(Collectors.groupingBy(ApprovalNodeConfigEntity::getApprovalFlowConfigId));
        Map<Long, Long> flowConfigId2Count = referencedApprovalFlowConfigId2Count();
        return flowConfigEntities.stream()
                .map(flowEntity -> entityToModel(flowEntity,
                        flowId2Nodes.getOrDefault(flowEntity.getId(), null), flowConfigId2Count))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_APPROVAL_FLOW_CONFIG", isForAll = true)
    public Boolean exists(@NotBlank String name) {
        ApprovalFlowConfigEntity example = new ApprovalFlowConfigEntity();
        example.setOrganizationId(authenticationFacade.currentOrganizationId());
        example.setName(name);
        return approvalFlowConfigRepository.exists(Example.of(example));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthenticate(actions = "create", resourceType = "ODC_APPROVAL_FLOW_CONFIG", isForAll = true)
    public ApprovalFlowConfig create(@NonNull ApprovalFlowConfig approvalFlowConfig) {
        checkApprovalNodeConfig(approvalFlowConfig.getNodes());
        ApprovalFlowConfigEntity entity = modelToEntity(approvalFlowConfig);
        ApprovalFlowConfigEntity flowEntity = approvalFlowConfigRepository.save(entity);
        List<ApprovalNodeConfigEntity> nodeEntities =
                nodeConfigRepository.saveAll(modelToEntity(approvalFlowConfig.getNodes(), flowEntity.getId()));
        return entityToModel(flowEntity, nodeEntities, referencedApprovalFlowConfigId2Count());
    }

    @SkipAuthorize("internal authenticated")
    public ApprovalFlowConfig detail(@NonNull Long id) {
        ApprovalFlowConfigEntity flowEntity = approvalFlowConfigRepository.findByOrganizationIdAndId(
                authenticationFacade.currentOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_APPROVAL_FLOW_CONFIG, "id", id));
        List<ApprovalNodeConfigEntity> nodeEntities =
                nodeConfigRepository.findByApprovalFlowConfigId(flowEntity.getId());
        return entityToModel(flowEntity, nodeEntities, referencedApprovalFlowConfigId2Count());
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_APPROVAL_FLOW_CONFIG", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public ApprovalFlowConfig update(@NonNull Long id, @NonNull ApprovalFlowConfig flowConfig) {
        checkApprovalNodeConfig(flowConfig.getNodes());
        ApprovalFlowConfigEntity saved =
                approvalFlowConfigRepository.findByOrganizationIdAndId(authenticationFacade.currentOrganizationId(), id)
                        .orElseThrow(() -> new NotFoundException(
                                ResourceType.ODC_APPROVAL_FLOW_CONFIG, "id", id));
        if (saved.getBuiltIn()) {
            throw new BadRequestException("cannot update built-in approval flow config");
        }
        saved.setName(flowConfig.getName());
        saved.setDescription(flowConfig.getDescription());
        saved.setApprovalExpirationIntervalSeconds(flowConfig.getApprovalExpirationIntervalSeconds());
        saved.setExecutionExpirationIntervalSeconds(flowConfig.getExecutionExpirationIntervalSeconds());
        saved.setWaitExecutionExpirationIntervalSeconds(flowConfig.getWaitExecutionExpirationIntervalSeconds());
        ApprovalFlowConfigEntity updated = approvalFlowConfigRepository.save(saved);
        nodeConfigRepository.deleteByApprovalFlowConfigId(id);
        nodeConfigRepository.flush();
        List<ApprovalNodeConfigEntity> nodeEntities =
                nodeConfigRepository.saveAll(modelToEntity(flowConfig.getNodes(), id));
        return entityToModel(updated, nodeEntities, referencedApprovalFlowConfigId2Count());
    }

    @PreAuthenticate(actions = "delete", resourceType = "ODC_APPROVAL_FLOW_CONFIG", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(@NonNull Long id) {
        ApprovalFlowConfigEntity saved =
                approvalFlowConfigRepository.findByOrganizationIdAndId(authenticationFacade.currentOrganizationId(), id)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_APPROVAL_FLOW_CONFIG, "id", id));
        if (saved.getBuiltIn()) {
            throw new BadRequestException("cannot delete built-in approval flow config");
        }
        for (Consumer<ApprovalFlowConfigDeleteEvent> hook : preDeleteHooks) {
            hook.accept(new ApprovalFlowConfigDeleteEvent(id));
        }
        nodeConfigRepository.deleteByApprovalFlowConfigId(saved.getId());
        approvalFlowConfigRepository.delete(saved);
        return true;
    }


    @SkipAuthorize("internal usage")
    private List<ApprovalFlowConfigEntity> listByOrganizationId(@NonNull Long organizationId) {
        return approvalFlowConfigRepository.findByOrganizationId(organizationId);
    }


    @SkipAuthorize("internal usage")
    public ApprovalFlowConfig findById(@NonNull Long id) {
        ApprovalFlowConfigEntity flowEntity = approvalFlowConfigRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_APPROVAL_FLOW_CONFIG, "id", id));
        List<ApprovalNodeConfigEntity> nodeEntities = nodeConfigRepository.findByApprovalFlowConfigId(id);
        return entityToModel(flowEntity, nodeEntities, referencedApprovalFlowConfigId2Count());
    }

    @SkipAuthorize("internal usage")
    public boolean exists(@NonNull Long organizationId, @NonNull Long id) {
        ApprovalFlowConfigEntity example = new ApprovalFlowConfigEntity();
        example.setOrganizationId(organizationId);
        example.setId(id);
        return approvalFlowConfigRepository.exists(Example.of(example));
    }

    @SkipAuthorize("internal usage")
    public List<ApprovalFlowConfigEntity> listRelatedFlowConfigByIntegrationId(@NonNull Long integrationId) {
        return approvalFlowConfigRepository.findByIntegrationId(integrationId);
    }

    @SkipAuthorize("odc internal usage")
    public void addPreApprovalFlowConfigDeleteHook(Consumer<ApprovalFlowConfigDeleteEvent> hook) {
        preDeleteHooks.add(hook);
    }

    private ApprovalFlowConfig entityToModel(ApprovalFlowConfigEntity flowEntity,
            List<ApprovalNodeConfigEntity> nodeEntities, @NonNull Map<Long, Long> referencedId2Count) {
        ApprovalFlowConfig model = approvalFlowConfigMapper.entityToModel(flowEntity);
        model.setReferencedCount(referencedId2Count.getOrDefault(model.getId(), 0L).intValue());
        if (CollectionUtils.isNotEmpty(nodeEntities)) {
            model.setNodes(nodeEntities.stream().map(node -> {
                ApprovalNodeConfig nodeConfig = approvalFlowNodeConfigMapper.entityToModel(node);
                if (Objects.nonNull(node.getResourceRoleId())) {
                    nodeConfig.setExternalApproval(false);
                    resourceRoleService.findResourceRoleById(node.getResourceRoleId()).ifPresent(
                            resourceRole -> nodeConfig.setResourceRoleName(resourceRole.getRoleName().name()));
                }
                if (Objects.nonNull(node.getExternalApprovalId())) {
                    nodeConfig.setExternalApproval(true);
                    integrationService.findIntegrationById(node.getExternalApprovalId()).ifPresent(
                            integration -> nodeConfig.setExternalApprovalName(integration.getName()));
                }
                return nodeConfig;
            }).filter(node -> Objects.nonNull(node.getSequenceNumber()))
                    .sorted(Comparator.comparing(ApprovalNodeConfig::getSequenceNumber)).collect(Collectors.toList()));
        }
        return model;
    }


    private Map<Long, Long> referencedApprovalFlowConfigId2Count() {
        return riskLevelRepository.findByOrganizationId(authenticationFacade.currentOrganizationId()).stream()
                .collect(Collectors.groupingBy(RiskLevelEntity::getApprovalFlowConfigId, Collectors.counting()));
    }

    private ApprovalFlowConfigEntity modelToEntity(ApprovalFlowConfig model) {
        ApprovalFlowConfigEntity entity = approvalFlowConfigMapper.modelToEntity(model);
        entity.setOrganizationId(authenticationFacade.currentOrganizationId());
        entity.setCreatorId(authenticationFacade.currentUserId());
        entity.setBuiltIn(false);
        return entity;
    }

    private List<ApprovalNodeConfigEntity> modelToEntity(@NonNull List<ApprovalNodeConfig> model,
            @NonNull Long flowConfigId) {
        final int[] order = {0};
        return model.stream().map(node -> {
            ApprovalNodeConfigEntity nodeConfigEntity = approvalFlowNodeConfigMapper.modelToEntity(node);
            nodeConfigEntity.setSequenceNumber(order[0]);
            order[0]++;
            nodeConfigEntity.setApprovalFlowConfigId(flowConfigId);
            return nodeConfigEntity;
        }).collect(Collectors.toList());
    }

    private void checkApprovalNodeConfig(@NonNull Collection<ApprovalNodeConfig> nodes) {
        nodes.forEach(node -> {
            if (Objects.nonNull(node.getExternalApproval()) && node.getExternalApproval()) {
                PreConditions.notNull(node.getExternalApprovalId(), "externalApprovalId");
                // Current user must hold "read" permission for ODC_INTEGRATION
                IntegrationConfig approvalIntegration = integrationService.detail(node.getExternalApprovalId());
                PreConditions.validExists(ResourceType.ODC_INTEGRATION, "id", node.getExternalApprovalId(),
                        () -> approvalIntegration != null);
            } else {
                PreConditions.notNull(node.getResourceRoleId(), "resourceRoleId");
                Optional<ResourceRoleEntity> resourceRole =
                        resourceRoleService.findResourceRoleById(node.getResourceRoleId());
                PreConditions.validExists(ResourceType.ODC_RESOURCE_ROLE, "id", node.getResourceRoleId(),
                        resourceRole::isPresent);
            }
        });
    }

    @Data
    @AllArgsConstructor
    public static class ApprovalFlowConfigDeleteEvent {
        private Long id;
    }

}
