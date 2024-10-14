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

import java.util.List;

import com.oceanbase.odc.common.dfa.DfaStateTransfer;
import com.oceanbase.odc.service.resource.ResourceState;
import com.oceanbase.odc.service.resource.k8s.model.K8sResource;

import lombok.Setter;

/**
 * {@link K8sResourceStatusTransfer}
 *
 * @author yh263208
 * @date 2024-09-11 16:10
 * @since ODC_release_4.3.2
 */
@Setter
public class K8sResourceStatusTransfer<T extends K8sResource> implements DfaStateTransfer<ResourceState, T> {

    private ResourceState from;
    private ResourceState next;
    private List<? extends K8sResourceMatcher<T>> k8sResourceMatchers;

    public static <T extends K8sResource> K8sResourceStatusTransferBuilder<T> builder() {
        return new K8sResourceStatusTransferBuilder<>();
    }

    @Override
    public ResourceState next() {
        return this.next;
    }

    @Override
    public boolean matchesState(ResourceState resourceState) {
        return this.from.equals(resourceState);
    }

    @Override
    public boolean matchesInput(T k8sResource) {
        return this.k8sResourceMatchers.stream().anyMatch(m -> m.matches(k8sResource));
    }

}
