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

/**
 * meta store of the resource
 * 
 * @author longpeng.zlp
 * @date 2024/8/12 11:32
 */
public interface ResourceMetaStore {
    /**
     * find if the resource has created
     * 
     * @param resourceID
     * @return
     */
    Resource findResource(ResourceID resourceID);

    /**
     * create the resource. Exception will throw if Resource has been saved
     * 
     * @param resource
     * @return
     */
    void saveResource(Resource resource) throws Exception;

    /**
     * update resource
     * 
     * @param resource
     * @return 1 if updated, 0 not updated
     */
    int updateResourceState(ResourceID resource, ResourceState resourceState);

    /**
     * remove the resource with resourceID
     * 
     * @param resourceID
     * @return 1 if resource exists and been removed. 0 if resource not exist
     */
    int deleteResource(ResourceID resourceID);
}
