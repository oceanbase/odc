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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.oceanbase.odc.common.dfa.AbstractDfa;
import com.oceanbase.odc.common.dfa.DfaStateTransfer;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sPod;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link K8sPodStatusDfa}
 *
 * @author yh263208
 * @date 2024-09-07 10:58
 * @since ODC_release_4.3.2
 */
@Slf4j
public class K8sPodStatusDfa extends AbstractDfa<ResourceState, K8sPod> {

    public static K8sPodStatusDfa getInstance() {
        List<DfaStateTransfer<ResourceState, K8sPod>> transfers = new ArrayList<>();
        ResourceState[] fromState = new ResourceState[] {
                ResourceState.CREATING, ResourceState.AVAILABLE, ResourceState.ERROR_STATE
        };
        transfers.addAll(K8sPodStatusTransfer.builder().from(fromState)
                .matchesPod(getCreatingPodMatchers()).to(ResourceState.CREATING).build());
        transfers.addAll(K8sPodStatusTransfer.builder().from(fromState)
                .matchesPod(getErrorPodMatchers()).to(ResourceState.ERROR_STATE).build());
        transfers.addAll(K8sPodStatusTransfer.builder().from(fromState)
                .matchesPod(getAvailablePodMatchers()).to(ResourceState.AVAILABLE).build());
        transfers.addAll(K8sPodStatusTransfer.builder().from(fromState)
                .matchesPod(getPodNonExistsMatchers()).to(ResourceState.UNKNOWN).build());
        transfers.addAll(K8sPodStatusTransfer.builder().from(ResourceState.DESTROYING)
                .matchesPod(getPodNonExistsMatchers()).to(ResourceState.DESTROYED).build());
        return new K8sPodStatusDfa(transfers);
    }

    private static List<K8sPodMatcher> getAvailablePodMatchers() {
        K8sPodMatcher m1 = new K8sPodMatcher();
        m1.setPodStatusIn(Collections.singleton("Running"));
        m1.setForAllContainers(true);
        m1.setContainerStatusIn(Collections.singleton(K8sPodContainerStatus.getRunningStatus()));
        return Collections.singletonList(m1);
    }

    private static List<K8sPodMatcher> getPodNonExistsMatchers() {
        K8sPodMatcher m1 = new K8sPodMatcher();
        m1.setMatchesNullK8sPod(true);
        return Collections.singletonList(m1);
    }

    private static List<K8sPodMatcher> getCreatingPodMatchers() {
        K8sPodMatcher m1 = new K8sPodMatcher();
        m1.setIgnoreContainerStatus(true);
        m1.setPodStatusIn(Collections.singleton("Pending"));

        K8sPodMatcher m2 = new K8sPodMatcher();
        m2.setPodStatusIn(Collections.singleton("Running"));
        m2.setForAllContainers(true);
        m2.setContainerStatusIn(K8sPodContainerStatus.getCreatingStatuses());

        K8sPodMatcher m3 = new K8sPodMatcher();
        m3.setPodStatusIn(Collections.singleton("Running"));
        m3.setForAllContainers(true);
        m3.setContainerStatusIn(K8sPodContainerStatus.getNonErrorStatuses());
        m3.setMinMatchesCountInHasContainerStatuses(1);
        m3.setHasContainerStatuses(K8sPodContainerStatus.getCreatingStatuses());
        return Arrays.asList(m1, m2, m3);
    }

    private static List<K8sPodMatcher> getErrorPodMatchers() {
        K8sPodMatcher m1 = new K8sPodMatcher();
        m1.setIgnoreContainerStatus(true);
        m1.setPodStatusIn(Collections.singleton("Failed"));

        K8sPodMatcher m2 = new K8sPodMatcher();
        m2.setIgnoreContainerStatus(true);
        m2.setPodStatusIn(Collections.singleton("Unknown"));

        K8sPodMatcher m3 = new K8sPodMatcher();
        m3.setPodStatusIn(Collections.singleton("Running"));
        m3.setForAnyContainers(true);
        m3.setMatchesAllContainerStatus(true);
        m3.setContainerStatusNotIn(K8sPodContainerStatus.getNonErrorStatuses());
        return Arrays.asList(m1, m2, m3);
    }

    public K8sPodStatusDfa(@NonNull List<DfaStateTransfer<ResourceState, K8sPod>> dfaStateTransfers) {
        super(dfaStateTransfers);
    }

    @Override
    protected void onStateTransfer(ResourceState currentState, ResourceState nextState, K8sPod pod) {
        log.info("The state has been changed, currentState={}, nextState={}", currentState, nextState);
    }

}
