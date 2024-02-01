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
package com.oceanbase.odc.metadb.partitionplan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * {@link PartitionPlanTablePartitionKeyRepository}
 *
 * @author yh263208
 * @date 2023-01-10 18:53
 * @since ODC_release_4.2.4
 */
public interface PartitionPlanTablePartitionKeyRepository
        extends JpaRepository<PartitionPlanTablePartitionKeyEntity, Long>,
        JpaSpecificationExecutor<PartitionPlanTablePartitionKeyEntity> {
}
