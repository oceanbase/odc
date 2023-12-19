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

package com.oceanbase.odc.service.flow.model;

import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.service.flow.instance.BaseFlowNodeInstance;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * {@link NodeInstanceEntityKey}
 *
 * @author yh263208
 * @date 2023-12-14 16:06
 * @since ODC_release_4.2.3
 */
@EqualsAndHashCode
public class NodeInstanceEntityKey {

    private final Long id;
    private final FlowNodeType nodeType;
    private final FlowableElementType coreType;

    public NodeInstanceEntityKey(@NonNull NodeInstanceEntity e) {
        this.id = e.getInstanceId();
        this.nodeType = e.getInstanceType();
        this.coreType = e.getFlowableElementType();
    }

    public NodeInstanceEntityKey(@NonNull BaseFlowNodeInstance e) {
        this.id = e.getId();
        this.nodeType = e.getNodeType();
        this.coreType = e.getCoreFlowableElementType();
    }

}
