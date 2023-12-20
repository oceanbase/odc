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

import org.apache.commons.lang.Validate;

import com.oceanbase.odc.service.flow.model.FlowNodeType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class FlowNodeInstanceKey {

    private final String shortUniqueId;
    private final Long instanceId;
    private final FlowNodeType instanceType;

    public FlowNodeInstanceKey(@NonNull BaseFlowNodeInstance instance) {
        Validate.notNull(instance.getId(), "Id for instance can not be null");
        this.instanceId = instance.getId();
        this.instanceType = instance.getNodeType();
        this.shortUniqueId = null;
    }

    public FlowNodeInstanceKey(@NonNull Long instanceId, @NonNull FlowNodeType instanceType) {
        this.instanceId = instanceId;
        this.instanceType = instanceType;
        this.shortUniqueId = null;
    }

    public FlowNodeInstanceKey(@NonNull String shortUniqueId, @NonNull FlowNodeType instanceType) {
        this.instanceId = null;
        this.instanceType = instanceType;
        this.shortUniqueId = shortUniqueId;
    }

}
