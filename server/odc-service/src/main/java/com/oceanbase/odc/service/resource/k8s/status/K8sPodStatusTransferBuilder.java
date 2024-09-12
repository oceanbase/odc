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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.oceanbase.odc.service.resource.ResourceState;

/**
 * {@link K8sPodStatusTransferBuilder}
 *
 * @author yh263208
 * @date 2024-09-11 17:21
 * @since ODC_release_4.3.2
 */
public class K8sPodStatusTransferBuilder {

    private Set<ResourceState> candidates = new HashSet<>();
    private ResourceState next;
    private List<K8sPodMatcher> k8sPodMatchers = new ArrayList<>();

    public List<K8sPodStatusTransfer> build() {
        return this.candidates.stream().map(state -> {
            K8sPodStatusTransfer transfer = new K8sPodStatusTransfer();
            transfer.setFrom(state);
            transfer.setNext(next);
            transfer.setK8sPodMatchers(k8sPodMatchers);
            return transfer;
        }).collect(Collectors.toList());
    }

    public K8sPodStatusTransferBuilder from(ResourceState... candidates) {
        this.candidates = new HashSet<>(Arrays.asList(candidates));
        return this;
    }

    public K8sPodStatusTransferBuilder matchesPod(List<K8sPodMatcher> k8sPodMatcher) {
        this.k8sPodMatchers = k8sPodMatcher;
        return this;
    }

    public K8sPodStatusTransferBuilder to(ResourceState next) {
        this.next = next;
        return this;
    }

}
