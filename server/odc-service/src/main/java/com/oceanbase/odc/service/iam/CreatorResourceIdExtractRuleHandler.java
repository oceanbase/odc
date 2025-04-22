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
package com.oceanbase.odc.service.iam;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.authority.model.DefaultSecurityResource;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.resourcegroup.model.ResourceContext;
import com.oceanbase.odc.service.resourcegroup.model.ResourceContext.ResourceIdExtractRule;

/**
 * @Author: Lebie
 * @Date: 2025/2/27 15:13
 * @Description: []
 */
@Component
public class CreatorResourceIdExtractRuleHandler implements ResourceIdExtractRuleHandler {
    @Autowired
    private ResourceHandlerFactory resourceHandlerFactory;

    @Override
    public boolean supports(ResourceIdExtractRule rule) {
        return ResourceIdExtractRule.CREATOR == rule;
    }

    @Override
    public List<SecurityResource> handle(ResourceContext context, AuthenticationFacade authFacade) {
        Set<Long> ids = resourceHandlerFactory.getHandler(context.getField())
                .findIds(authFacade.currentOrganizationId(), authFacade.currentUserId());
        return ids.stream()
                .map(id -> new DefaultSecurityResource(String.valueOf(id), context.getField()))
                .collect(Collectors.toList());
    }
}
