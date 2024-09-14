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
package com.oceanbase.odc.service.resource.k8s.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sDeployment;
import com.oceanbase.odc.service.resource.k8s.model.K8sPod;

import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import lombok.NonNull;
import lombok.Setter;

/**
 * {@link K8sDeploymentMatcher}
 *
 * @author yh263208
 * @date 2024-09-12 17:13
 * @since ODC_release_4.3.2
 */
@Setter
public class K8sDeploymentMatcher implements K8sResourceMatcher<K8sDeployment> {

    private boolean ignoreReplicasCount;
    private boolean replicasEnough;
    private boolean forAllPods;
    private Set<ResourceState> podStatusIn = new HashSet<>();
    private Integer minMatchesCountInHasPodStatuses = null;
    private Set<ResourceState> hasPodStatuses = new HashSet<>();
    private boolean matchesAllPodStatus;
    private Set<ResourceState> podStatusNotIn = new HashSet<>();
    private final ResourceState currentState;

    public K8sDeploymentMatcher(@NonNull ResourceState currentState) {
        this.currentState = currentState;
    }

    public void setForAnyPods(boolean forAnyPods) {
        this.forAllPods = !forAnyPods;
    }

    public void setReplicasNonEnough(boolean replicasNonEnough) {
        this.replicasEnough = !replicasNonEnough;
    }

    @Override
    public boolean matches(K8sDeployment k8sResource) {
        if (k8sResource == null) {
            return false;
        }
        V1DeploymentSpec spec = k8sResource.getSpec();
        Validate.notNull(spec);
        Validate.notNull(spec.getReplicas());
        List<K8sPod> k8sPodList;
        try {
            k8sPodList = k8sResource.getK8sPods();
        } catch (Exception e) {
            return false;
        }
        if (CollectionUtils.isEmpty(k8sPodList)) {
            return false;
        }
        boolean matches = true;
        int replicas = spec.getReplicas();
        if (this.replicasEnough) {
            matches &= (k8sPodList.size() >= replicas);
        } else if (!this.ignoreReplicasCount) {
            matches &= (k8sPodList.size() < replicas);
        }
        if (this.forAllPods) {
            matches &= k8sPodList.stream().allMatch(this::matchesPodStatus);
            if (CollectionUtils.isNotEmpty(this.hasPodStatuses)) {
                matches &= ifReachesMinMatchesCountInHasPodStatuses(k8sPodList);
            }
        } else {
            matches &= k8sPodList.stream().anyMatch(this::matchesPodStatus);
        }
        return matches;
    }

    private boolean ifReachesMinMatchesCountInHasPodStatuses(List<K8sPod> k8sPodList) {
        int matchesCount = matchesCountInHasPodStatuses(k8sPodList);
        if (minMatchesCountInHasPodStatuses == null || minMatchesCountInHasPodStatuses <= 0) {
            return matchesCount >= hasPodStatuses.size();
        }
        return matchesCount >= minMatchesCountInHasPodStatuses;
    }

    private int matchesCountInHasPodStatuses(List<K8sPod> k8sPodList) {
        int matchesCount = 0;
        for (ResourceState item : this.hasPodStatuses) {
            if (k8sPodList.stream().map(this::getPodStatus).collect(Collectors.toList()).contains(item)) {
                matchesCount++;
            }
        }
        return matchesCount;
    }

    private ResourceState getPodStatus(K8sPod target) {
        try {
            return K8sPodStatusDfa.buildInstance().next(target, currentState);
        } catch (Exception e) {
            return ResourceState.UNKNOWN;
        }
    }

    private boolean matchesPodStatus(K8sPod target) {
        ResourceState targetState = getPodStatus(target);
        if (this.matchesAllPodStatus) {
            if (CollectionUtils.isNotEmpty(this.podStatusNotIn)) {
                return !CollectionUtils.containsAny(this.podStatusNotIn, targetState);
            }
            return true;
        } else if (CollectionUtils.isNotEmpty(this.podStatusIn)) {
            return CollectionUtils.containsAny(this.podStatusIn, targetState);
        }
        return false;
    }

}
