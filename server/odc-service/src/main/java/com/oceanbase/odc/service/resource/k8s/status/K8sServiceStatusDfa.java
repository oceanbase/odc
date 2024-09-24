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
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.oceanbase.odc.common.dfa.AbstractDfa;
import com.oceanbase.odc.common.dfa.DfaStateTransfer;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sService;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link K8sServiceStatusDfa}
 *
 * @author yh263208
 * @date 2024-09-12 16:12
 * @since ODC_release_4.3.2
 */
@Slf4j
public class K8sServiceStatusDfa extends AbstractDfa<ResourceState, K8sService> {

    public static K8sServiceStatusDfa buildInstance() {
        List<DfaStateTransfer<ResourceState, K8sService>> transfers = new ArrayList<>();
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sService>()
                .from(ResourceState.CREATING)
                .matchesK8sResource(getNullMatchers()).to(ResourceState.CREATING).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sService>()
                .from(ResourceState.AVAILABLE)
                .matchesK8sResource(getNullMatchers()).to(ResourceState.UNKNOWN).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sService>()
                .from(ResourceState.CREATING)
                .matchesK8sResource(getNonNullMatchers()).to(ResourceState.AVAILABLE).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sService>()
                .from(ResourceState.AVAILABLE)
                .matchesK8sResource(getNonNullMatchers()).to(ResourceState.AVAILABLE).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sService>()
                .from(ResourceState.DESTROYING)
                .matchesK8sResource(getNullMatchers()).to(ResourceState.DESTROYED).build());
        transfers.addAll(new K8sResourceStatusTransferBuilder<K8sService>()
                .from(ResourceState.DESTROYING)
                .matchesK8sResource(getNonNullMatchers()).to(ResourceState.DESTROYING).build());
        return new K8sServiceStatusDfa(transfers);
    }

    private static List<K8sResourceMatcher<K8sService>> getNullMatchers() {
        return Collections.singletonList(Objects::isNull);
    }

    private static List<K8sResourceMatcher<K8sService>> getNonNullMatchers() {
        return Collections.singletonList(Objects::nonNull);
    }

    public K8sServiceStatusDfa(@NonNull List<DfaStateTransfer<ResourceState, K8sService>> dfaStateTransfers) {
        super(dfaStateTransfers);
    }

    @Override
    protected void onStateTransfer(ResourceState currentState, ResourceState nextState, K8sService k8sService) {
        log.info("The state has been changed, currentState={}, nextState={}", currentState, nextState);
    }

}
