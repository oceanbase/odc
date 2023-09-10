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

package com.oceanbase.odc.service.collaboration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.iam.UserOrganizationEntity;
import com.oceanbase.odc.metadb.iam.UserOrganizationRepository;
import com.oceanbase.odc.service.iam.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/9/10 00:59
 * @Description: []
 */
@Slf4j
@Service
@Validated
@SkipAuthorize
public class UserOrganizationMigrator {
    @Autowired
    private UserOrganizationRepository userOrganizationRepository;

    @Transactional(rollbackFor = Exception.class)
    public void migrate(User user) {
        if (!userOrganizationRepository.existsByOrganizationIdAndUserId(user.getOrganizationId(), user.getId())) {
            UserOrganizationEntity userOrganizationEntity = new UserOrganizationEntity();
            userOrganizationEntity.setOrganizationId(user.getOrganizationId());
            userOrganizationEntity.setUserId(user.getId());
            userOrganizationRepository.saveAndFlush(userOrganizationEntity);
        }
    }
}
