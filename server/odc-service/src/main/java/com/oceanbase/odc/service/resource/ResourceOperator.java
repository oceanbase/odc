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

import java.util.Optional;
import java.util.function.Function;

import com.oceanbase.odc.service.task.exception.JobException;

/**
 * manager resource create / recycle / destroy
 * 
 * @author longpeng.zlp
 * @date 2024/8/12 11:42
 */
public interface ResourceOperator<T extends ResourceContext> {
    /**
     * current only use resource type to determinate which type should be created create may not real
     * create
     * 
     * @param resourceContext resource config
     * @return resource, may not available, maybe creating
     */
    Resource create(T resourceContext) throws JobException;

    /**
     * query if resource existed
     * 
     * @param resourceID
     * @return
     */
    Optional<? extends Resource> query(ResourceID resourceID) throws JobException;

    /**
     * destroy resource with resourceID destroy may not real destroy
     * 
     * @param resourceID
     * @return
     */
    String destroy(ResourceID resourceID) throws JobException;

    /**
     * detect if resource can be destroyed
     * 
     * @param resourceID
     * @param createElapsedTimeFunc provide created time in seconds if needed
     * @return true if resource can be destroyed
     */
    boolean canBeDestroyed(ResourceID resourceID, Function<ResourceID, Long> createElapsedTimeFunc);
}
