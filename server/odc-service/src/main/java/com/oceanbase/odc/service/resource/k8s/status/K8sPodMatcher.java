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

import com.oceanbase.odc.service.resource.k8s.model.K8sPod;

import io.kubernetes.client.openapi.models.V1ContainerStatus;
import lombok.Setter;

/**
 * {@link K8sPodMatcher}
 *
 * @author yh263208
 * @date 2024-09-11 18:18
 * @since ODC_release_4.3.2
 */
@Setter
public class K8sPodMatcher implements K8sResourceMatcher<K8sPod> {

    private Set<String> podStatusIn = new HashSet<>();
    private boolean matchesAllPodStatus;
    private Set<String> podStatusNotIn = new HashSet<>();

    private boolean ignoreContainerStatus;
    private boolean noContainerStatus;
    private boolean forAllContainers;
    private Set<K8sPodContainerStatus> containerStatusIn = new HashSet<>();
    private Integer minMatchesCountInHasContainerStatuses = null;
    private Set<K8sPodContainerStatus> hasContainerStatuses = new HashSet<>();
    private boolean matchesAllContainerStatus;
    private Set<K8sPodContainerStatus> containerStatusNotIn = new HashSet<>();

    public void setForAnyContainers(boolean forAnyContainers) {
        this.forAllContainers = !forAnyContainers;
    }

    @Override
    public boolean matches(K8sPod k8sPod) {
        if (k8sPod == null) {
            return false;
        }
        Validate.notNull(k8sPod.getStatus());
        Validate.notNull(k8sPod.getStatus().getPhase());
        String phase = k8sPod.getStatus().getPhase();
        boolean podMatches;
        if (this.matchesAllPodStatus) {
            if (CollectionUtils.isNotEmpty(this.podStatusNotIn)) {
                podMatches = !CollectionUtils.containsAny(this.podStatusNotIn.stream()
                        .map(String::toUpperCase).collect(Collectors.toList()), phase.toUpperCase());
            } else {
                podMatches = true;
            }
        } else if (CollectionUtils.isNotEmpty(this.podStatusIn)) {
            podMatches = CollectionUtils.containsAny(this.podStatusIn.stream()
                    .map(String::toUpperCase).collect(Collectors.toList()), phase.toUpperCase());
        } else {
            return false;
        }
        boolean containersMatches;
        if (this.ignoreContainerStatus) {
            containersMatches = true;
        } else if (CollectionUtils.isEmpty(k8sPod.getStatus().getContainerStatuses())) {
            containersMatches = this.noContainerStatus;
        } else {
            List<V1ContainerStatus> cs = k8sPod.getStatus().getContainerStatuses();
            if (this.forAllContainers) {
                containersMatches = cs.stream().allMatch(s -> matchesPodContainerStatus(new K8sPodContainerStatus(s)));
                if (CollectionUtils.isNotEmpty(this.hasContainerStatuses)) {
                    containersMatches &= ifReachesMinMatchesCountInHasContainerStatuses(cs);
                }
            } else {
                containersMatches = cs.stream().anyMatch(s -> matchesPodContainerStatus(new K8sPodContainerStatus(s)));
            }
        }
        return containersMatches && podMatches;
    }

    private boolean ifReachesMinMatchesCountInHasContainerStatuses(List<V1ContainerStatus> cs) {
        int matchesCount = matchesCountInHasContainerStatuses(cs);
        if (this.minMatchesCountInHasContainerStatuses == null || this.minMatchesCountInHasContainerStatuses <= 0) {
            return matchesCount >= this.hasContainerStatuses.size();
        }
        return matchesCount >= this.minMatchesCountInHasContainerStatuses;
    }

    private int matchesCountInHasContainerStatuses(List<V1ContainerStatus> cs) {
        int matchesCount = 0;
        for (K8sPodContainerStatus item : this.hasContainerStatuses) {
            if (cs.stream().map(K8sPodContainerStatus::new).collect(Collectors.toList()).contains(item)) {
                matchesCount++;
            }
        }
        return matchesCount;
    }

    private boolean matchesPodContainerStatus(K8sPodContainerStatus target) {
        if (this.matchesAllContainerStatus) {
            if (CollectionUtils.isNotEmpty(this.containerStatusNotIn)) {
                return !CollectionUtils.containsAny(this.containerStatusNotIn, target);
            }
            return true;
        } else if (CollectionUtils.isNotEmpty(this.containerStatusIn)) {
            return CollectionUtils.containsAny(this.containerStatusIn, target);
        }
        return false;
    }

}
