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
package com.oceanbase.odc.metadb.resource;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oceanbase.odc.service.resource.model.QueryResourceParams;
import com.oceanbase.odc.service.resource.model.ResourceState;

import lombok.NonNull;

public interface ResourceRepository {

    Page<ResourceEntity> findAll(@NonNull QueryResourceParams params, @NonNull Pageable pageable);

    ResourceEntity save(ResourceEntity entity);

    Optional<ResourceEntity> findById(@NonNull Long id);

    int updateResourceIDById(@NonNull Long id, @NonNull String resourceIDJson);

    int updateResourceStateById(@NonNull Long id, @NonNull ResourceState resourceState);

    int updateResourceStateIdIn(@NonNull List<Long> ids, @NonNull ResourceState resourceState);

}
