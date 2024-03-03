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
package com.oceanbase.odc.core.flow;

import java.util.Optional;

import com.oceanbase.odc.core.flow.builder.BaseProcessNodeBuilder;
import com.oceanbase.odc.core.flow.builder.ConditionSequenceFlowBuilder;
import com.oceanbase.odc.core.flow.builder.EndEventBuilder;
import com.oceanbase.odc.core.flow.builder.ExclusiveGatewayBuilder;
import com.oceanbase.odc.core.flow.builder.FlowableProcessBuilder;
import com.oceanbase.odc.core.flow.builder.ParallelGatewayBuilder;
import com.oceanbase.odc.core.flow.builder.SequenceFlowBuilder;
import com.oceanbase.odc.core.flow.graph.GraphConfigurer;
import com.oceanbase.odc.core.flow.graph.GraphEdge;

import lombok.NonNull;
import lombok.Setter;

/**
 * Impl of {@link ExecutionConfigurer}
 *
 * @author yh263208
 * @date 2022-01-24 21:47
 * @since ODC_release_3.3.0
 * @see ExecutionConfigurer
 */
public class ExecutionConfigurer extends GraphConfigurer<FlowableProcessBuilder, BaseProcessNodeBuilder<?>> {

    /**
     * previous GraphVertex set out GraphEdge but not set target GraphVertex
     */
    @Setter
    private GraphEdge previousGraphEdge;

    public ExecutionConfigurer(@NonNull FlowableProcessBuilder target) {
        super(target);
    }

    public ExecutionConfigurer next(@NonNull BaseProcessNodeBuilder<?> nextNode) {
        BaseProcessNodeBuilder<?> from = last();
        if (from instanceof EndEventBuilder) {
            throw new IllegalStateException("Can not append node after EndEvent");
        }
        ExecutionConfigurer executionConfigurer = (ExecutionConfigurer) super.next(nextNode,
                Optional.ofNullable(previousGraphEdge).orElse(
                        new SequenceFlowBuilder(
                                from == null ? "" : from.getGraphId() + " -> " + nextNode.getGraphId())));
        if (previousGraphEdge != null) {
            previousGraphEdge = null;
        }
        return executionConfigurer;
    }

    public ExecutionConfigurer route(@NonNull String expr, @NonNull ExecutionConfigurer configurer) {
        BaseProcessNodeBuilder<?> to = configurer.first();
        if (to == null) {
            return this;
        }
        BaseProcessNodeBuilder<?> from = last();
        if (!(from instanceof ExclusiveGatewayBuilder)) {
            throw new IllegalStateException("Last node has to be a instance of ExclusiveGateway");
        }
        return (ExecutionConfigurer) super.route(
                new ConditionSequenceFlowBuilder(from.getGraphId() + " -> " + to.getGraphId(), expr), configurer);
    }

    public ExecutionConfigurer route(@NonNull ExecutionConfigurer configurer) {
        BaseProcessNodeBuilder<?> to = configurer.first();
        if (to == null) {
            return this;
        }
        BaseProcessNodeBuilder<?> from = last();
        if (!(from instanceof ParallelGatewayBuilder) && !(from instanceof ExclusiveGatewayBuilder)) {
            throw new IllegalStateException("Last node has to be a instance of gateway");
        }
        return (ExecutionConfigurer) super.route(new SequenceFlowBuilder(from.getGraphId() + " -> " + to.getGraphId()),
                configurer);
    }

    public ExecutionConfigurer endProcess() {
        return endProcess(null);
    }

    public ExecutionConfigurer endProcess(Class<? extends BaseExecutionListener> endListenerClazz) {
        EndEventBuilder endEventBuilder = new EndEventBuilder();
        if (endListenerClazz != null) {
            endEventBuilder.addExecutionListener(endListenerClazz);
        }
        return next(endEventBuilder);
    }

}
