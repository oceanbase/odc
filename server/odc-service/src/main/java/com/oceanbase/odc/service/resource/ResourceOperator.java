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

import org.apache.hadoop.classification.InterfaceStability.Evolving;

import com.oceanbase.odc.service.task.exception.JobException;

/**
 * operator resource create / recycle / destroy
 * 
 * @param <RC> config of resource creating
 * @param <R> resource describe
 * @author longpeng.zlp
 * @date 2024/8/12 11:42
 */
@Evolving
public interface ResourceOperator<RC extends ResourceContext, R extends Resource> {
    /**
     * create a resource by resource context, create may not real create
     * 
     * @param resourceContext resource config
     * @return resource, may not available, maybe creating
     */
    R create(RC resourceContext) throws JobException;

    /**
     * modify a resource with resourceID
     * 
     * @param resourceContext
     * @return
     * @throws JobException
     */
    R patch(ResourceID resourceID, RC resourceContext) throws JobException;

    /**
     * query if resource with resource id existed
     * 
     * @param resourceID
     * @return
     */
    Optional<R> query(ResourceID resourceID) throws JobException;

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
     * @return true if resource can be destroyed
     */
    boolean canBeDestroyed(ResourceID resourceID);
}
