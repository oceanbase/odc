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
package com.oceanbase.odc.service.regulation.ruleset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.regulation.ruleset.RuleApplyingRepository;
import com.oceanbase.odc.metadb.regulation.ruleset.RulesetEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RulesetRepository;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.regulation.ruleset.model.Ruleset;

import lombok.NonNull;

/**
 * @Author: Lebie
 * @Date: 2023/5/29 15:08
 * @Description: []
 */
@Service
@Authenticated
public class RulesetService {
    @Autowired
    private RulesetRepository rulesetRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    @Autowired
    private RuleApplyingRepository ruleApplyingRepository;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    private final RulesetMapper rulesetMapper = RulesetMapper.INSTANCE;

    @SkipAuthorize("internal authenticated")
    public Ruleset detail(@NonNull Long id) {
        Ruleset ruleset = entityToModel(rulesetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RULESET, "id", id)));
        permissionValidator.checkCurrentOrganization(ruleset);
        return ruleset;
    }

    @SkipAuthorize("internal authenticated")
    @Transactional(rollbackFor = Exception.class)
    public Ruleset create(@NonNull Ruleset ruleset) {
        permissionValidator.checkCurrentOrganization(ruleset);
        return entityToModel(rulesetRepository.save(modelToEntity(ruleset)));
    }

    @SkipAuthorize("internal authenticated")
    @Transactional(rollbackFor = Exception.class)
    public Ruleset delete(@NonNull Long id) {
        Ruleset ruleset = entityToModel(rulesetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RULESET, "id", id)));
        permissionValidator.checkCurrentOrganization(ruleset);
        rulesetRepository.deleteById(id);
        ruleApplyingRepository.deleteByOrganizationIdAndRulesetId(authenticationFacade.currentOrganizationId(), id);
        return ruleset;
    }

    private Ruleset entityToModel(RulesetEntity entity) {
        return rulesetMapper.entityToModel(entity);
    }

    private RulesetEntity modelToEntity(Ruleset ruleset) {
        return rulesetMapper.modelToEntity(ruleset);
    }
}
