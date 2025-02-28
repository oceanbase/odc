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
package com.oceanbase.odc.service.task.resource.manager;

import java.util.List;

import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.metadb.task.SupervisorEndpointEntity;

/**
 * task manage strategy, extend this strategy if needed
 * 
 * @author longpeng.zlp
 * @date 2024/12/20 09:40
 */
public interface ResourceManageStrategy {

    /**
     * handle no resource available for allocate request
     *
     * @param resourceAllocateInfoEntity
     * @return resource id preparing
     */
    SupervisorEndpointEntity handleNoResourceAvailable(ResourceAllocateInfoEntity resourceAllocateInfoEntity)
            throws Exception;

    /**
     * detect if resource is ready for new resource
     *
     * @param resourceAllocateInfoEntity
     * @return resource endpoint if ready, else null
     */
    SupervisorEndpointEntity detectIfEndpointIsAvailable(
            ResourceAllocateInfoEntity resourceAllocateInfoEntity);

    void refreshSupervisorEndpoint(SupervisorEndpointEntity endpoint);

    /**
     * if supervisorEndpoint have enough resource for allocate request
     *
     * @param supervisorEndpoint
     * @return
     */
    boolean isEndpointHaveEnoughResource(SupervisorEndpointEntity supervisorEndpoint,
            ResourceAllocateInfoEntity entity);

    /**
     * release resource for given endpoint
     * 
     * @param endpoint
     */
    void releaseResourceById(SupervisorEndpointEntity endpoint);

    /**
     * pick up resource expect to released
     * 
     * @param entity
     * @return
     */
    List<SupervisorEndpointEntity> pickReleasedEndpoint(List<SupervisorEndpointEntity> entity);
}
