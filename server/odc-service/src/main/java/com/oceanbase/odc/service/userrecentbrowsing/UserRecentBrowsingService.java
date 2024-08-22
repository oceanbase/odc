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
package com.oceanbase.odc.service.userrecentbrowsing;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.metadb.userrecentbrowsing.UserRecentBrowsingEntity;
import com.oceanbase.odc.metadb.userrecentbrowsing.UserRecentBrowsingRepository;

@Service
public class UserRecentBrowsingService {

    private final UserRecentBrowsingRepository userRecentBrowsingRepository;

    @Autowired
    public UserRecentBrowsingService(UserRecentBrowsingRepository userRecentBrowsingRepository) {
        this.userRecentBrowsingRepository = userRecentBrowsingRepository;
    }

    public UserRecentBrowsingEntity add(Long projectId, Long userId, BrowseItemType itemType,
            Long itemId, Date browseTime) {
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(userId, "userId");
        PreConditions.notNull(itemType, "itemType");
        PreConditions.notNull(itemId, "itemId");
        UserRecentBrowsingEntity entity =
                userRecentBrowsingRepository.findByProjectIdAndUserIdAndItemTypeAndItemId(projectId, userId,
                        itemType.name(), itemId);
        if (entity == null) {
            entity = UserRecentBrowsingEntity.builder().projectId(projectId)
                    .userId(userId).itemType(itemType.name()).itemId(itemId)
                    .browseTime(browseTime).build();
        } else {
            entity.setBrowseTime(browseTime);
        }
        userRecentBrowsingRepository.save(entity);
        return entity;
    }

    public UserRecentBrowsingEntity getByProjectAndUserAndItem(Long projectId, Long userId, BrowseItemType itemType,
            Long itemId) {
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(userId, "userId");
        PreConditions.notNull(itemType, "itemType");
        PreConditions.notNull(itemId, "itemId");
        return userRecentBrowsingRepository.findByProjectIdAndUserIdAndItemTypeAndItemId(projectId, userId,
                itemType.name(), itemId);
    }

    public List<UserRecentBrowsingEntity> listRecentBrowsingEntitiesWithLimit(Long projectId, Long userId,
            List<BrowseItemType> itemTypes, int limit) {
        PreConditions.notNull(projectId, "projectId");
        PreConditions.notNull(userId, "userId");
        PreConditions.notEmpty(itemTypes, "itemTypes");
        PreConditions.validArgumentState(limit > 0, ErrorCodes.BadArgument, new Object[] {},
                "limit should be greater than 0");
        List<UserRecentBrowsingEntity> result = new ArrayList<>();
        for (BrowseItemType itemType : itemTypes) {
            List<UserRecentBrowsingEntity> userRecentBrowsingEntities =
                    userRecentBrowsingRepository.listRecentBrowsingWithLimit(projectId, userId, itemType.name(), limit);
            if (CollectionUtils.isNotEmpty(userRecentBrowsingEntities)) {
                result.addAll(userRecentBrowsingEntities);
            }
        }
        return itemTypes.size() <= 1 ? result
                : result.stream().sorted((o1, o2) -> o2.getBrowseTime().compareTo(o1.getBrowseTime())).limit(limit)
                        .collect(Collectors.toList());
    }
}
