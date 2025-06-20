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
package com.oceanbase.odc.service.task.resource.manager.strategy.process;

import java.util.Collections;
import java.util.List;

import com.oceanbase.odc.metadb.task.ResourceAllocateInfoEntity;
import com.oceanbase.odc.metadb.task.SupervisorEndpointEntity;
import com.oceanbase.odc.service.task.resource.manager.ResourceManageStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * default process resource manage strategy
 * 
 * @author longpeng.zlp
 * @date 2024/12/2 14:43
 */
@Slf4j
public class ProcessResourceManageStrategy implements ResourceManageStrategy {
    public ProcessResourceManageStrategy() {}

    /**
     * handle no resource available for allocate request
     * 
     * @param resourceAllocateInfoEntity
     * @return info to stored
     */
    public SupervisorEndpointEntity handleNoResourceAvailable(ResourceAllocateInfoEntity resourceAllocateInfoEntity)
            throws Exception {
        return null;
    }

    /**
     * detect if resource is ready for new resource
     * 
     * @param resourceAllocateInfoEntity
     * @return resource endpoint if ready, else null
     */
    public SupervisorEndpointEntity detectIfEndpointIsAvailable(ResourceAllocateInfoEntity resourceAllocateInfoEntity) {
        return null;
    }

    @Override
    public void refreshSupervisorEndpoint(SupervisorEndpointEntity endpoint) {}

    @Override
    public boolean isEndpointHaveEnoughResource(SupervisorEndpointEntity supervisorEndpoint,
            ResourceAllocateInfoEntity entity) {
        // TODO(lx): add resource detect logic here
        return true;
    }

    @Override
    public void releaseResourceById(SupervisorEndpointEntity endpoint) {
        throw new RuntimeException("not support yet");
    }

    // nothing released
    @Override
    public List<SupervisorEndpointEntity> pickReleasedEndpoint(List<SupervisorEndpointEntity> entity) {
        return Collections.emptyList();
    }

}
