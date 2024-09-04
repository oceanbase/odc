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
package com.oceanbase.odc.service.resource;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.resource.model.QueryResourceParams;
import com.oceanbase.odc.service.resource.model.ResourceState;

import lombok.NonNull;

// just for test
@Deprecated
public class InMemoryResourceRepository implements ResourceRepository {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);
    private static final Map<Long, ResourceEntity> ID_2_RESOURCE = new HashMap<>();

    @Override
    public Page<ResourceEntity> findAll(@NonNull QueryResourceParams params, @NonNull Pageable pageable) {
        List<ResourceEntity> returnVal = ID_2_RESOURCE.values().stream()
                .sorted(Comparator.comparing(ResourceEntity::getId)).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(params.getIds())) {
            returnVal = returnVal.stream()
                    .filter(entity -> params.getIds().contains(entity.getId()))
                    .collect(Collectors.toList());
        }
        PageRequest request = PageRequest.of(returnVal.size(), ID_2_RESOURCE.size(), Sort.by(Direction.ASC, "id"));
        return new PageImpl<>(returnVal, request, ID_2_RESOURCE.size());
    }

    @Override
    public ResourceEntity save(ResourceEntity entity) {
        Long id = ID_GENERATOR.getAndDecrement();
        entity.setId(id);
        ID_2_RESOURCE.put(id, entity);
        return entity;
    }

    @Override
    public Optional<ResourceEntity> findById(@NonNull Long id) {
        if (!ID_2_RESOURCE.containsKey(id)) {
            return Optional.empty();
        }
        return Optional.of(ID_2_RESOURCE.get(id));
    }

    public int updateResourceStateById(@NonNull Long id, @NonNull ResourceState resourceState) {
        if (!ID_2_RESOURCE.containsKey(id)) {
            return 0;
        }
        ID_2_RESOURCE.get(id).setResourceState(resourceState);
        return 1;
    }

    @Override
    public int updateResourceStateIdIn(@NonNull List<Long> ids, @NonNull ResourceState resourceState) {
        int total = 0;
        for (Long id : ids) {
            total += updateResourceStateById(id, resourceState);
        }
        return total;
    }

    @Override
    public int updateResourceIDById(@NonNull Long id, @NonNull String resourceIDJson) {
        if (!ID_2_RESOURCE.containsKey(id)) {
            return 0;
        }
        ID_2_RESOURCE.get(id).setResourceIDJson(resourceIDJson);
        return 1;
    }

}
