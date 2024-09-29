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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import com.oceanbase.odc.common.dfa.AbstractDfa;
import com.oceanbase.odc.common.dfa.DfaStateTransfer;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sDeployment;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link K8sDeploymentStatusDfa}
 *
 * @author yh263208
 * @date 2024-09-12 17:48
 * @since ODC_release_4.3.2
 */
@Slf4j
public class K8sDeploymentStatusDfa extends AbstractDfa<ResourceState, K8sDeployment> {

    public static K8sDeploymentStatusDfa buildInstance(ResourceState current) {
        List<DfaStateTransfer<ResourceState, K8sDeployment>> transfers = new ArrayList<>();
        ResourceState[] fromState = new ResourceState[] {
                ResourceState.CREATING, ResourceState.AVAILABLE, ResourceState.ERROR_STATE
        };
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sDeployment>().from(fromState)
                .matchesK8sResource(getCreatingDeploymentMatchers(current)).to(ResourceState.CREATING).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sDeployment>().from(ResourceState.CREATING)
                .matchesK8sResource(Collections.singletonList(Objects::isNull)).to(ResourceState.CREATING).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sDeployment>().from(fromState)
                .matchesK8sResource(getErrorDeploymentMatchers(current)).to(ResourceState.ERROR_STATE).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sDeployment>().from(fromState)
                .matchesK8sResource(getAvailableDeploymentMatchers(current)).to(ResourceState.AVAILABLE).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sDeployment>()
                .from(ResourceState.AVAILABLE, ResourceState.ERROR_STATE)
                .matchesK8sResource(Collections.singletonList(Objects::isNull)).to(ResourceState.UNKNOWN).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sDeployment>().from(ResourceState.DESTROYING)
                .matchesK8sResource(Collections.singletonList(Objects::isNull)).to(ResourceState.DESTROYED).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sDeployment>().from(ResourceState.DESTROYING)
                .matchesK8sResource(Collections.singletonList(Objects::nonNull)).to(ResourceState.DESTROYING).build());
        return new K8sDeploymentStatusDfa(transfers);
    }

    private static List<K8sDeploymentMatcher> getAvailableDeploymentMatchers(ResourceState resourceState) {
        K8sDeploymentMatcher m1 = new K8sDeploymentMatcher(resourceState);
        m1.setIgnoreReplicasCount(false);
        m1.setReplicasEnough(true);
        m1.setForAllPods(true);
        m1.setPodStatusIn(new HashSet<>(Collections.singletonList(ResourceState.AVAILABLE)));
        return Collections.singletonList(m1);
    }

    private static List<K8sDeploymentMatcher> getCreatingDeploymentMatchers(ResourceState resourceState) {
        K8sDeploymentMatcher m1 = new K8sDeploymentMatcher(resourceState);
        m1.setIgnoreReplicasCount(false);
        m1.setReplicasNonEnough(true);
        m1.setForAllPods(true);
        m1.setPodStatusIn(new HashSet<>(Arrays.asList(ResourceState.CREATING, ResourceState.AVAILABLE)));

        K8sDeploymentMatcher m2 = new K8sDeploymentMatcher(resourceState);
        m2.setIgnoreReplicasCount(true);
        m2.setForAllPods(true);
        m2.setPodStatusIn(new HashSet<>(Arrays.asList(ResourceState.CREATING, ResourceState.AVAILABLE)));
        m2.setMinMatchesCountInHasPodStatuses(1);
        m2.setHasPodStatuses(Collections.singleton(ResourceState.CREATING));

        K8sDeploymentMatcher m3 = new K8sDeploymentMatcher(resourceState);
        m3.setReplicasEmpty(true);
        return Arrays.asList(m1, m2, m3);
    }

    private static List<K8sDeploymentMatcher> getErrorDeploymentMatchers(ResourceState resourceState) {
        K8sDeploymentMatcher m1 = new K8sDeploymentMatcher(resourceState);
        m1.setIgnoreReplicasCount(true);
        m1.setForAnyPods(true);
        m1.setPodStatusIn(Collections.singleton(ResourceState.ERROR_STATE));
        return Collections.singletonList(m1);
    }

    public K8sDeploymentStatusDfa(@NonNull List<DfaStateTransfer<ResourceState, K8sDeployment>> dfaStateTransfers) {
        super(dfaStateTransfers);
    }

    @Override
    protected void onStateTransfer(ResourceState currentState, ResourceState nextState, K8sDeployment k8sDeployment) {
        log.debug("The state has been changed, currentState={}, nextState={}", currentState, nextState);
    }

}
