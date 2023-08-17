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
package com.oceanbase.odc.service.automation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.automation.AutomationConditionEntity;
import com.oceanbase.odc.metadb.automation.AutomationConditionRepository;
import com.oceanbase.odc.service.automation.model.AutomationCondition;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class AutomationConditionService {
    @Autowired
    private AutomationConditionRepository conditionRepository;
    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Transactional(rollbackFor = Exception.class)
    public AutomationCondition insert(Long ruleId, AutomationCondition condition) {
        AutomationConditionEntity conditionEntity = new AutomationConditionEntity(condition);
        conditionEntity.setRuleId(ruleId);
        conditionEntity.setCreatorId(authenticationFacade.currentUserId());
        conditionEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
        conditionEntity.setEnabled(true);
        AutomationConditionEntity savedEntity = conditionRepository.saveAndFlush(conditionEntity);
        return AutomationCondition.of(savedEntity);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<AutomationCondition> insertAll(Long ruleId, List<AutomationCondition> conditions) {
        return conditions.stream()
                .map(condition -> {
                    AutomationConditionEntity entity = new AutomationConditionEntity(condition);
                    entity.setRuleId(ruleId);
                    entity.setCreatorId(authenticationFacade.currentUserId());
                    entity.setOrganizationId(authenticationFacade.currentOrganizationId());
                    entity.setEnabled(true);
                    return AutomationCondition.of(conditionRepository.saveAndFlush(entity));
                })
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public AutomationCondition delete(Long id) {
        Optional<AutomationConditionEntity> optional = conditionRepository.findById(id);
        if (!optional.isPresent()) {
            throw new BadRequestException("Auto triggered condition not found by id=" + id);
        }
        conditionRepository.deleteById(id);
        return AutomationCondition.of(optional.get());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<AutomationCondition> deleteByRuleId(Long ruleId) {
        List<AutomationConditionEntity> automationConditionEntities = conditionRepository.deleteByRuleId(ruleId);
        return automationConditionEntities
                .stream().map(AutomationCondition::of).collect(Collectors.toList());
    }
}
