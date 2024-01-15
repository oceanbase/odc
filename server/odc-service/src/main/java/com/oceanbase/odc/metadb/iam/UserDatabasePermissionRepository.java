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
package com.oceanbase.odc.metadb.iam;

import java.util.Collection;
import java.util.List;

import com.oceanbase.odc.metadb.flow.ReadOnlyRepository;

/**
 * @author gaoda.xy
 * @date 2024/1/3 17:38
 */
public interface UserDatabasePermissionRepository extends ReadOnlyRepository<UserDatabasePermissionEntity, Long> {

    List<UserDatabasePermissionEntity> findByProjectIdAndIdIn(Long projectId, Collection<Long> ids);

    List<UserDatabasePermissionEntity> findByUserIdAndDatabaseIdIn(Long userId, Collection<Long> databaseIds);

}
