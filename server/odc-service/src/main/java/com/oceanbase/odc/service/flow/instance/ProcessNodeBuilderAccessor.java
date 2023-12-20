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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.bpmn.model.FlowNode;

import com.oceanbase.odc.core.flow.builder.BaseProcessNodeBuilder;
import com.oceanbase.odc.core.shared.Verify;

import lombok.NonNull;

/**
 * {@link ProcessNodeBuilderAccessor}
 *
 * @author yh263208
 * @date 2022-02-22 16:54
 * @since ODC_release_3.3.0
 */
public class ProcessNodeBuilderAccessor {

    private final Map<FlowNodeInstanceKey, Map<String, BaseProcessNodeBuilder<?>>> nodeInstance2NodeBuilders =
            new ConcurrentHashMap<>();

    /**
     * Bind the {@link BaseProcessNodeBuilder} to {@link BaseFlowNodeInstance}
     *
     * @param nodeBuilder target {@link BaseProcessNodeBuilder}
     * @param nodeInstance target {@link BaseFlowNodeInstance}
     */
    public void setNodeBuilder(@NonNull BaseFlowNodeInstance nodeInstance,
            @NonNull BaseProcessNodeBuilder<? extends FlowNode> nodeBuilder) {
        FlowNodeInstanceKey key = new FlowNodeInstanceKey(nodeInstance.getShortUniqueId(), nodeInstance.getNodeType());
        Map<String, BaseProcessNodeBuilder<?>> name2NodeBuilder =
                nodeInstance2NodeBuilders.computeIfAbsent(key, subKey -> new HashMap<>());
        String builderName = nodeBuilder.getName();
        Verify.verify(!name2NodeBuilder.containsKey(builderName), "Duplicate name for one instance " + key);
        name2NodeBuilder.putIfAbsent(builderName, nodeBuilder);
    }

    /**
     * Get {@link BaseProcessNodeBuilder} by {@link BaseProcessNodeBuilder#getName()}
     *
     * @param name refers to {@link BaseProcessNodeBuilder#getName()}
     * @param instance refers to {@link BaseFlowNodeInstance}
     * @return {@link BaseProcessNodeBuilder}
     */
    public Optional<BaseProcessNodeBuilder<? extends FlowNode>> getNodeBuilderByName(@NonNull String name,
            @NonNull BaseFlowNodeInstance instance) {
        FlowNodeInstanceKey key = new FlowNodeInstanceKey(instance.getShortUniqueId(), instance.getNodeType());
        Map<String, BaseProcessNodeBuilder<?>> name2NodeBuilder = nodeInstance2NodeBuilders.get(key);
        if (name2NodeBuilder == null) {
            return Optional.empty();
        }
        BaseProcessNodeBuilder<?> nodeBuilder = name2NodeBuilder.get(name);
        if (nodeBuilder == null) {
            return Optional.empty();
        }
        return Optional.of(nodeBuilder);
    }

}
