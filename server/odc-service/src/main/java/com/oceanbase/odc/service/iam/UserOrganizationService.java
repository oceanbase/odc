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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.metadb.iam.UserOrganizationEntity;
import com.oceanbase.odc.metadb.iam.UserOrganizationRepository;
import com.oceanbase.odc.metadb.iam.UserRepository;
import com.oceanbase.odc.service.iam.model.User;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2023/5/4 19:32
 * @Description: []
 */
@Service
@Slf4j
public class UserOrganizationService {
    @Autowired
    private UserOrganizationRepository userOrganizationRepository;

    @Autowired
    private UserRepository userRepository;

    @SkipAuthorize
    public boolean userBelongsToOrganization(@NonNull Long userId, @NonNull Long organizationId) {
        UserOrganizationEntity userOrganizationEntity = new UserOrganizationEntity();
        userOrganizationEntity.setUserId(userId);
        userOrganizationEntity.setOrganizationId(organizationId);
        return userOrganizationRepository.exists(Example.of(userOrganizationEntity))
                || userRepository.existsByIdAndOrganizationId(userId, organizationId);
    }

    @SkipAuthorize
    @Transactional(rollbackFor = Exception.class)
    public UserOrganizationEntity bindUserToOrganization(@NonNull Long userId, @NonNull Long organizationId) {
        UserOrganizationEntity entity = new UserOrganizationEntity();
        entity.setUserId(userId);
        entity.setOrganizationId(organizationId);
        return userOrganizationRepository.save(entity);
    }

    @SkipAuthorize
    public List<User> listUsersByOrganizationId(@NonNull Long organizationId) {
        List<UserOrganizationEntity> entities = userOrganizationRepository.findByOrganizationId(organizationId);
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        return userRepository
                .findByIdIn(entities.stream().map(UserOrganizationEntity::getUserId).collect(Collectors.toList()))
                .stream().map(User::new).collect(Collectors.toList());
    }
}
