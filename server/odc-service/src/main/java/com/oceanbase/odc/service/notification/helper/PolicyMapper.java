/*
 * Copyright (c) 2024 OceanBase.
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
package com.oceanbase.odc.service.notification.helper;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.metadb.notification.NotificationPolicyEntity;
import com.oceanbase.odc.service.notification.model.NotificationPolicy;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/11
 */
@Component
public class PolicyMapper {

    public NotificationPolicy fromEntity(NotificationPolicyEntity entity) {
        NotificationPolicy policy = new NotificationPolicy();
        policy.setId(entity.getId());
        policy.setTitleTemplate(entity.getTitleTemplate());
        policy.setContentTemplate(entity.getContentTemplate());
        policy.setMatchExpression(entity.getMatchExpression());
        policy.setCreateTime(entity.getCreateTime());
        policy.setUpdateTime(entity.getUpdateTime());
        policy.setCreatorId(entity.getCreatorId());
        policy.setOrganizationId(entity.getOrganizationId());
        policy.setProjectId(entity.getProjectId());
        policy.setPolicyMetadataId(entity.getPolicyMetadataId());
        policy.setEnabled(entity.isEnabled());
        policy.setToUsers(entity.getToUsers());
        policy.setCcUsers(entity.getCcUsers());
        return policy;
    }

}
