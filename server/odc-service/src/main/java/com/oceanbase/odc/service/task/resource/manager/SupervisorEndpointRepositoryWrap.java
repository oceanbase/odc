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
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.metadb.task.SupervisorEndpointEntity;
import com.oceanbase.odc.metadb.task.SupervisorEndpointRepository;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.oceanbase.odc.service.task.supervisor.SupervisorEndpointState;
import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2024/12/19 14:37
 */
@Slf4j
public class SupervisorEndpointRepositoryWrap {
    protected final SupervisorEndpointRepository repository;

    public SupervisorEndpointRepositoryWrap(SupervisorEndpointRepository repository) {
        this.repository = repository;
    }

    public void abandonSupervisorEndpoint(SupervisorEndpointEntity endpoint) {
        this.repository.updateStatusById(endpoint.getId(), SupervisorEndpointState.ABANDON.name());
    }

    public void onlineSupervisorEndpoint(SupervisorEndpointEntity endpoint) {
        this.repository.updateStatusById(endpoint.getId(), SupervisorEndpointState.AVAILABLE.name());
    }

    public void offSupervisorEndpoint(SupervisorEndpointEntity endpoint) {
        this.repository.updateStatusById(endpoint.getId(), SupervisorEndpointState.UNAVAILABLE.name());
    }

    public SupervisorEndpointEntity save(K8sPodResource k8sPodResource, long resourceID, int initLoad) {
        SupervisorEndpointEntity endpoint = new SupervisorEndpointEntity();
        endpoint.setPort(Integer.valueOf(k8sPodResource.getServicePort()));
        endpoint.setHost(k8sPodResource.getPodIpAddress());
        endpoint.setStatus(SupervisorEndpointState.PREPARING.name());
        endpoint.setResourceGroup(k8sPodResource.getGroup());
        endpoint.setResourceRegion(k8sPodResource.getRegion());
        endpoint.setResourceID(resourceID);
        endpoint.setLoads(initLoad);
        return repository.save(endpoint);
    }

    public SupervisorEndpointEntity getSupervisorEndpointState(SupervisorEndpoint endpoint, long resourceID) {
        Optional<SupervisorEndpointEntity> supervisorEndpointEntity =
                repository.findByHostPortAndResourceId(endpoint.getHost(), endpoint.getPort(), resourceID);
        if (!supervisorEndpointEntity.isPresent()) {
            throw new RuntimeException("resource not found. endpoint=" + resourceID + ", resourceID =" + resourceID);
        } else {
            return supervisorEndpointEntity.get();
        }
    }

    public void releaseLoad(SupervisorEndpoint supervisorEndpoint, long resourceID) {
        Optional<SupervisorEndpointEntity> optionalSupervisorEndpointEntity = repository
                .findByHostPortAndResourceId(supervisorEndpoint.getHost(),
                        Integer.valueOf(supervisorEndpoint.getPort()), resourceID);
        if (!optionalSupervisorEndpointEntity.isPresent()) {
            log.warn("update supervisor endpoint failed, endpoint={}", supervisorEndpoint);
            return;
        }
        SupervisorEndpointEntity supervisorEndpointEntity = optionalSupervisorEndpointEntity.get();
        operateLoad(supervisorEndpointEntity.getHost(),
                supervisorEndpointEntity.getPort(), resourceID, -1);
    }

    public void operateLoad(String host, int port, long resourceID, int delta) {
        repository.addLoadByHostPortAndResourceId(host, port, resourceID, delta);
    }

    public List<SupervisorEndpointEntity> collectAvailableSupervisorEndpoint(String region, String group) {
        Specification<SupervisorEndpointEntity> condition = Specification.where(
                SpecificationUtil.columnEqual("status", SupervisorEndpointState.AVAILABLE.name()));
        condition = condition.and(SpecificationUtil.columnEqual("resourceRegion", region))
                .and(SpecificationUtil.columnEqual("resourceGroup", group));
        return repository.findAll(condition, PageRequest.of(0, 100)).getContent();
    }

    public List<SupervisorEndpointEntity> collectIdleAvailableSupervisorEndpoint() {
        Specification<SupervisorEndpointEntity> condition = Specification.where(
                SpecificationUtil.columnEqual("status", SupervisorEndpointState.AVAILABLE.name()));
        condition = condition.and(SpecificationUtil.columnLessThanOrEqualTo("loads", 0L));
        return repository.findAll(condition, PageRequest.of(0, 100)).getContent();
    }

    public List<SupervisorEndpointEntity> collectPreparingSupervisorEndpoint() {
        Specification<SupervisorEndpointEntity> condition = Specification.where(
                SpecificationUtil.columnEqual("status", SupervisorEndpointState.PREPARING.name()));
        return repository.findAll(condition, PageRequest.of(0, 100)).getContent();
    }
}
