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
package com.oceanbase.odc.metadb.flow;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

/**
 * @author liuyizhuo.lyz
 * @date 2024/2/27
 */
public interface FlowInstanceApprovalViewRepository extends ReadOnlyRepository<FlowInstanceApprovalViewEntity, Long> {

    @Query(value = "select * from flow_instance_approval_view where flow_instance_id=?1 order by id desc",
            nativeQuery = true)
    List<FlowInstanceApprovalViewEntity> findByFlowInstanceId(Long flowInstanceId);

}
