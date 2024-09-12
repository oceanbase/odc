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
import com.oceanbase.odc.service.resource.k8s.model.K8sPod;

import lombok.Setter;

/**
 * {@link K8sPodStatusTransfer}
 *
 * @author yh263208
 * @date 2024-09-11 16:10
 * @since ODC_release_4.3.2
 */
@Setter
public class K8sPodStatusTransfer implements DfaStateTransfer<ResourceState, K8sPod> {

    private ResourceState from;
    private ResourceState next;
    private List<K8sPodMatcher> k8sPodMatchers;

    public static K8sPodStatusTransferBuilder builder() {
        return new K8sPodStatusTransferBuilder();
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
    public boolean matchesInput(K8sPod k8sPod) {
        return this.k8sPodMatchers.stream().anyMatch(m -> m.matches(k8sPod));
    }

}
