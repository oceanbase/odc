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

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RulesetEntity;
import com.oceanbase.odc.metadb.regulation.ruleset.RulesetRepository;
import com.oceanbase.odc.service.iam.HorizontalDataPermissionValidator;
import com.oceanbase.odc.service.iam.UserService;
import com.oceanbase.odc.service.regulation.ruleset.model.QueryRuleMetadataParams;
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
    private RuleService ruleService;

    @Autowired
    private UserService userService;

    @Autowired
    private HorizontalDataPermissionValidator permissionValidator;

    private final RulesetMapper rulesetMapper = RulesetMapper.INSTANCE;

    @SkipAuthorize("internal authenticated")
    public Ruleset detail(@NonNull Long id) {
        Ruleset ruleset = entityToModel(rulesetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ResourceType.ODC_RULESET, "id", id)));
        permissionValidator.checkCurrentOrganization(ruleset);
        ruleset.setRules(ruleService.list(ruleset.getId(), QueryRuleMetadataParams.builder().build()));
        return ruleset;
    }

    private Ruleset entityToModel(RulesetEntity entity) {
        Ruleset ruleset = rulesetMapper.entityToModel(entity);
        UserEntity creator = userService.nullSafeGet(entity.getCreatorId());
        UserEntity lastModifier = userService.nullSafeGet(entity.getLastModifierId());
        if (Objects.nonNull(ruleset.getCreator())) {
            ruleset.getCreator().setName(creator.getName());
            ruleset.getCreator().setAccountName(creator.getAccountName());
        }
        if (Objects.nonNull(ruleset.getLastModifier())) {
            ruleset.getLastModifier().setName(lastModifier.getName());
            ruleset.getLastModifier().setAccountName(lastModifier.getAccountName());
        }
        return ruleset;
    }
}
