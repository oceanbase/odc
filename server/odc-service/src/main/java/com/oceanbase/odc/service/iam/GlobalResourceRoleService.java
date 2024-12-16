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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.constant.ResourceRoleName;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.iam.UserRoleRepository;
import com.oceanbase.odc.service.iam.model.UserGlobalResourceRole;
import com.oceanbase.odc.service.iam.util.GlobalResourceRoleUtil;

/**
 * @Author: Lebie
 * @Date: 2024/11/19 15:47
 * @Description: []
 */
@Service
public class GlobalResourceRoleService {

    @Autowired
    private UserRoleRepository userRoleRepository;

    public List<UserGlobalResourceRole> findGlobalResourceRoleUsersByOrganizationId(Long organizationId) {
        return userRoleRepository.findByOrganizationIdAndNameIn(
                organizationId,
                Arrays.asList(GlobalResourceRoleUtil.GLOBAL_PROJECT_OWNER, GlobalResourceRoleUtil.GLOBAL_PROJECT_DBA,
                        GlobalResourceRoleUtil.GLOBAL_PROJECT_SECURITY_ADMINISTRATOR));
    }

    public List<UserGlobalResourceRole> findGlobalResourceRoleUsersByOrganizationIdAndUserId(Long organizationId,
            Long userId) {
        return userRoleRepository.findByOrganizationIdAndUserIdAndNameIn(organizationId, userId,
                Arrays.asList(GlobalResourceRoleUtil.GLOBAL_PROJECT_OWNER, GlobalResourceRoleUtil.GLOBAL_PROJECT_DBA,
                        GlobalResourceRoleUtil.GLOBAL_PROJECT_SECURITY_ADMINISTRATOR));
    }


    public List<UserGlobalResourceRole> findGlobalResourceRoleUsersByOrganizationIdAndRole(Long organizationId,
            ResourceType resourceType, ResourceRoleName resourceRoleName) {
        if (resourceType != ResourceType.ODC_PROJECT) {
            return Collections.emptyList();
        }
        return userRoleRepository.findByOrganizationIdAndNameIn(
                organizationId, Arrays.asList(GlobalResourceRoleUtil.getGlobalRoleName(resourceRoleName)));
    }
}
