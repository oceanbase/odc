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
package com.oceanbase.odc.service.regulation.risklevel;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelEntity;
import com.oceanbase.odc.metadb.regulation.risklevel.RiskLevelRepository;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.approval.ApprovalFlowConfigService;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevel;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/6/15 16:43
 * @Description: []
 */
@Service("RiskLevelServiceFrom420")
@Authenticated
public class RiskLevelService {
    private final RiskLevelMapper riskLevelMapper = RiskLevelMapper.INSTANCE;

    @Autowired
    private RiskLevelRepository riskLevelRepository;

    @Autowired
    private ApprovalFlowConfigService approvalFlowConfigService;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @SkipAuthorize("internal authenticated")
    public List<RiskLevel> list() {
        return listByOrganizationId(authenticationFacade.currentOrganizationId());
    }

    @SkipAuthorize("internal authenticated")
    public RiskLevel detail(@NonNull Long id) {
        return entityToModel(
                riskLevelRepository.findByOrganizationIdAndId(authenticationFacade.currentOrganizationId(), id)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RISK_LEVEL, "id", id)));
    }

    @PreAuthenticate(actions = "update", resourceType = "ODC_RISK_LEVEL", indexOfIdParam = 0)
    @Transactional(rollbackFor = Exception.class)
    public RiskLevel update(@NonNull Long id, @NonNull RiskLevel riskLevel) {
        RiskLevelEntity saved =
                riskLevelRepository.findByOrganizationIdAndId(authenticationFacade.currentOrganizationId(), id)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RISK_LEVEL, "id", id));
        if (!approvalFlowConfigService.exists(authenticationFacade.currentOrganizationId(),
                riskLevel.getApprovalFlowConfigId())) {
            throw new NotFoundException(ResourceType.ODC_APPROVAL_FLOW_CONFIG, "id",
                    riskLevel.getApprovalFlowConfigId());
        }
        saved.setDescription(riskLevel.getDescription());
        saved.setApprovalFlowConfigId(riskLevel.getApprovalFlowConfigId());
        RiskLevelEntity updated = riskLevelRepository.save(saved);
        return entityToModel(updated);
    }

    @SkipAuthorize("internal usage")
    public RiskLevel findDefaultRiskLevel() {
        return entityToModel(
                riskLevelRepository.findByOrganizationIdAndLevel(authenticationFacade.currentOrganizationId(), 0)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RISK_LEVEL, "level", 0)));
    }

    @SkipAuthorize("internal usage")
    public RiskLevel findHighestRiskLevel() {
        return entityToModel(
                riskLevelRepository.findByOrganizationIdAndLevel(authenticationFacade.currentOrganizationId(), 3)
                        .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RISK_LEVEL, "level", 3)));
    }

    @SkipAuthorize("internal authenticated")
    public RiskLevel findHighestRiskLevel(Collection<RiskLevel> riskLevels) {
        if (CollectionUtils.isEmpty(riskLevels)) {
            return findDefaultRiskLevel();
        }
        return riskLevels.stream().filter(riskLevel -> Objects.nonNull(riskLevel.getLevel()))
                .max(Comparator.comparingInt(RiskLevel::getLevel)).orElse(findDefaultRiskLevel());
    }

    @SkipAuthorize("internal usage")
    public Optional<RiskLevel> findById(@NonNull Long id) {
        return riskLevelRepository.findById(id).map(this::entityToModel);
    }


    @SkipAuthorize("internal usage")
    public boolean exists(@NonNull Long organizationId, @NonNull Long id) {
        RiskLevelEntity example = new RiskLevelEntity();
        example.setOrganizationId(organizationId);
        example.setId(id);
        return riskLevelRepository.exists(Example.of(example));
    }

    @SkipAuthorize("internal usage")
    public List<RiskLevel> listByOrganizationId(@NonNull Long organizationId) {
        return riskLevelRepository.findByOrganizationId(organizationId).stream().map(this::entityToModel)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("internal usage")
    public List<RiskLevel> listRelatedRiskLevelByFlowConfigId(@NonNull Long flowConfigId) {
        return riskLevelRepository.findByApprovalFlowConfigId(flowConfigId).stream().map(this::entityToModel)
                .collect(Collectors.toList());
    }

    @SkipAuthorize("internal usage")
    public boolean isDefaultRiskLevel(@NonNull Long organizationId, @NonNull Long id) {
        Optional<RiskLevelEntity> riskLevelOptional = riskLevelRepository.findByOrganizationIdAndId(organizationId, id);
        if (!riskLevelOptional.isPresent()) {
            throw new NotFoundException(ResourceType.ODC_RISK_LEVEL, "id", id);
        }
        return riskLevelOptional.get().getLevel() == 0;
    }

    private RiskLevel entityToModel(RiskLevelEntity entity) {
        RiskLevel model = riskLevelMapper.entityToModel(entity);
        model.setApprovalFlowConfig(approvalFlowConfigService.findById(entity.getApprovalFlowConfigId()));
        return model;
    }
}
