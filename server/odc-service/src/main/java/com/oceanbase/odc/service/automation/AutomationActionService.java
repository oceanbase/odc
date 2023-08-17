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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.metadb.automation.AutomationActionEntity;
import com.oceanbase.odc.metadb.automation.AutomationActionRepository;
import com.oceanbase.odc.service.automation.model.AutomationAction;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@SkipAuthorize("odc internal usage")
public class AutomationActionService {
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private AutomationActionRepository actionRepository;
    @Autowired
    private AutomationActionChecker checker;

    @Transactional(rollbackFor = Exception.class)
    public AutomationAction insert(Long ruleId, AutomationAction action) {
        checker.check(action.getAction(), action.getArguments());
        AutomationActionEntity actionEntity = new AutomationActionEntity(action);
        actionEntity.setRuleId(ruleId);
        actionEntity.setCreatorId(authenticationFacade.currentUserId());
        actionEntity.setOrganizationId(authenticationFacade.currentOrganizationId());
        actionEntity.setEnabled(true);
        AutomationActionEntity savedEntity = actionRepository.saveAndFlush(actionEntity);
        return AutomationAction.of(savedEntity);
    }

    @Transactional(rollbackFor = Exception.class)
    public List<AutomationAction> insertAll(Long ruleId, List<AutomationAction> actions) {
        long currentUserId = authenticationFacade.currentUserId();
        List<AutomationAction> results = new ArrayList<>(actions.size());
        for (AutomationAction action : actions) {
            checker.check(action.getAction(), action.getArguments());
            AutomationActionEntity entity = new AutomationActionEntity(action);
            entity.setRuleId(ruleId);
            entity.setCreatorId(currentUserId);
            entity.setOrganizationId(authenticationFacade.currentOrganizationId());
            entity.setEnabled(true);
            results.add(AutomationAction.of(actionRepository.saveAndFlush(entity)));
        }
        return results;
    }

    @Transactional(rollbackFor = Exception.class)
    public AutomationAction delete(Long id) {
        Optional<AutomationActionEntity> optional = actionRepository.findById(id);
        if (!optional.isPresent()) {
            throw new BadRequestException("Auto triggered action not found by id=" + id);
        }
        actionRepository.deleteById(id);
        return AutomationAction.of(optional.get());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<AutomationAction> deleteByRuleId(Long ruleId) {
        List<AutomationActionEntity> automationActionEntities = actionRepository.deleteByRuleId(ruleId);
        return automationActionEntities
                .stream().map(AutomationAction::of).collect(Collectors.toList());
    }

}
