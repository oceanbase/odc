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
package com.oceanbase.odc.service.resource.k8s;

import java.util.Date;
import java.util.Optional;

import com.oceanbase.odc.metadb.resource.GlobalUniqueResourceID;
import com.oceanbase.odc.metadb.resource.ResourceEntity;
import com.oceanbase.odc.metadb.resource.ResourceRepository;
import com.oceanbase.odc.service.resource.ResourceMode;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.exception.JobException;

import lombok.extern.slf4j.Slf4j;

/**
 * resource manager to holds resource allocate and free
 * 
 * @author longpeng.zlp
 * @date 2024/8/26 20:17
 */
@Slf4j
public abstract class K8sResourceManager {
    /**
     * meta store of resource
     */
    protected final ResourceRepository resourceRepository;

    public K8sResourceManager(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * register or update k8s operator
     */
    public abstract void registerK8sOperator(String key, K8sResourceOperatorBuilder operator);

    /**
     * directly create k8s resource
     * 
     * @param k8sResourceContext context to create pod
     * @return
     */
    public K8sPodResource createK8sResource(K8sResourceContext k8sResourceContext) throws JobException {
        K8SResourceOperator operator = getK8sOperator(k8sResourceContext.getRegion(), k8sResourceContext.getGroup());
        K8sPodResource k8sResource = operator.create(k8sResourceContext);
        // if save resource to db failed, rollback it
        try {
            resourceRepository.save(createK8sResourceToResourceEntity(k8sResource));
        } catch (Throwable e) {
            log.info("save resource={} failed, rollback creation", k8sResource);
            // release resource if save db failed
            operator.destroy(k8sResource.id());
            throw new JobException("save resource to meta store failed", e);
        }
        return k8sResource;
    }

    /**
     * convert k8s resource to resource entiry
     * 
     * @param k8sResource
     * @return
     */
    protected ResourceEntity createK8sResourceToResourceEntity(K8sPodResource k8sResource) {
        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setResourceMode(ResourceMode.REMOTE_K8S);
        resourceEntity.setEndpoint(k8sResource.endpoint().getResourceURL());
        resourceEntity.setCreateTime(k8sResource.createDate());
        resourceEntity.setRegion(k8sResource.getRegion());
        resourceEntity.setGroupName(k8sResource.getGroup());
        resourceEntity.setNamespace(k8sResource.getNamespace());
        resourceEntity.setResourceName(k8sResource.getArn());
        resourceEntity.setStatus(k8sResource.getResourceState());
        return resourceEntity;
    }

    /**
     * query resource state with resource id
     * 
     * @param resourceID
     * @return
     * @throws JobException
     */
    public Optional<K8sPodResource> query(K8sPodResourceID resourceID) throws JobException {
        K8SResourceOperator operator = getK8sOperator(resourceID.getRegion(), resourceID.getGroup());
        return operator.query(resourceID);
    }

    /**
     * release resource, currently mark resource as destroying
     *
     * @param resourceID
     */
    public void release(K8sPodResourceID resourceID) {
        // first detect if resourceID is created, cause may be it's resource create by old task version
        // update job destroyed, let scheduler DestroyExecutorJob scan and destroy it
        Optional<ResourceEntity> savedResource = resourceRepository.findByResourceID(resourceID);
        if (!savedResource.isPresent()) {
            // create task_resource with DESTROYING state
            K8sPodResource k8sResource = new K8sPodResource(resourceID.getRegion(), resourceID.getGroup(),
                    resourceID.getNamespace(), resourceID.getName(), ResourceState.DESTROYING, "unknown",
                    new Date(System.currentTimeMillis()));
            resourceRepository.save(createK8sResourceToResourceEntity(k8sResource));
        } else {
            // update resource state to destroying
            resourceRepository.updateResourceStatus(resourceID, ResourceState.DESTROYING.name());
        }
    }

    public String destroy(GlobalUniqueResourceID resourceID) throws JobException {
        K8SResourceOperator operator = getK8sOperator(resourceID.getRegion(), resourceID.getGroup());
        String ret = operator.destroy(ResourceIDUtil.wrapToK8sResourceID(resourceID));
        // then update db status
        resourceRepository.updateResourceStatus(resourceID, ResourceState.DESTROYED.name());
        log.info("resourceID={} destroyed success", resourceID);
        return ret;
    }

    /**
     * detect if resource can be destroyed
     * 
     * @param resourceID
     * @return
     */
    public boolean canBeDestroyed(K8sPodResourceID resourceID) {
        K8SResourceOperator operator = getK8sOperator(resourceID.getRegion(), resourceID.getGroup());
        return operator.canBeDestroyed(resourceID);
    }

    /**
     * find resource operator by region
     * 
     * @param region
     * @return
     */
    protected abstract K8SResourceOperator getK8sOperator(String region, String group);
}
