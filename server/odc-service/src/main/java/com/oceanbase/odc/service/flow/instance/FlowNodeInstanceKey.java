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
package com.oceanbase.odc.service.flow.instance;

import com.oceanbase.odc.service.flow.model.FlowNodeType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class FlowNodeInstanceKey {
    private final Long instanceId;
    private final FlowNodeType instanceType;

    public FlowNodeInstanceKey(@NonNull BaseFlowNodeInstance instance) {
        this.instanceId = instance.getId();
        this.instanceType = instance.getNodeType();
    }

    public FlowNodeInstanceKey(@NonNull Long instanceId, @NonNull FlowNodeType instanceType) {
        this.instanceId = instanceId;
        this.instanceType = instanceType;
    }

}
