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
package com.oceanbase.odc.core.flow.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SequenceFlow;

import com.oceanbase.odc.core.flow.BaseExecutionListener;
import com.oceanbase.odc.core.flow.ExecutionConfigurer;
import com.oceanbase.odc.core.flow.ProcessElementBuilder;
import com.oceanbase.odc.core.flow.graph.Graph;
import com.oceanbase.odc.core.flow.graph.GraphEdge;
import com.oceanbase.odc.core.flow.graph.GraphVertex;
import com.oceanbase.odc.core.flow.util.FlowConstants;
import com.oceanbase.odc.core.flow.util.FlowIdGenerators;

import lombok.Getter;
import lombok.NonNull;

/**
 * Builder for {@link org.flowable.bpmn.model.Process}
 *
 * @author yh263208
 * @date 2022-01-14 15:42
 * @since ODC_release_3.3.0
 */
@Getter
public class FlowableProcessBuilder extends Graph implements ProcessElementBuilder<Process> {

    private final String name;
    private final boolean executable;
    private boolean started = false;
    private StartEventBuilder startEventBuilder = null;
    private final Set<String> listenerClassNames = new HashSet<>();

    public FlowableProcessBuilder(@NonNull String name, boolean executable) {
        this.name = name;
        this.executable = executable;
    }

    public FlowableProcessBuilder(@NonNull String name) {
        this(name, true);
    }

    public FlowableProcessBuilder addExecutionListener(
            @NonNull Class<? extends BaseExecutionListener> listenerClass) {
        this.listenerClassNames.add(listenerClass.getName());
        return this;
    }

    public ExecutionConfigurer newProcess() {
        return newProcess(null);
    }

    public ExecutionConfigurer newProcess(Class<? extends BaseExecutionListener> startListenerClazz) {
        if (started) {
            throw new IllegalStateException("Process Build has been started");
        }
        started = true;
        startEventBuilder = new StartEventBuilder();
        if (startListenerClazz != null) {
            startEventBuilder.addExecutionListener(startListenerClazz);
        }
        return newExecution(startEventBuilder);
    }

    public ExecutionConfigurer endProcess() {
        return endProcess(null);
    }

    public ExecutionConfigurer endProcess(Class<? extends BaseExecutionListener> endListenerClazz) {
        EndEventBuilder endEventBuilder = new EndEventBuilder();
        if (endListenerClazz != null) {
            endEventBuilder.addExecutionListener(endListenerClazz);
        }
        return newExecution(endEventBuilder);
    }

    public ExecutionConfigurer newExecution(BaseProcessNodeBuilder<?> newNode) {
        ExecutionConfigurer configurer = newExecution();
        configurer.next(newNode);
        return configurer;
    }

    public ExecutionConfigurer newExecution() {
        return new ExecutionConfigurer(this);
    }

    public FlowableProcessBuilder converge(@NonNull List<ExecutionConfigurer> configurerList,
            @NonNull ExecutionConfigurer convergedExecution) {
        BaseProcessNodeBuilder<?> to = convergedExecution.first();
        if (to == null) {
            throw new IllegalStateException("Dest execution can not be empty");
        }
        for (ExecutionConfigurer executionConfigurer : configurerList) {
            executionConfigurer.next(to);
        }
        return this;
    }

    @Override
    public Process build() {
        if (!started || startEventBuilder == null) {
            throw new IllegalStateException("No StartEvent exist");
        }
        getTopoOrderedVertices();
        Process returnValue = new Process();
        Map<String, SequenceFlow> flowId2Flow = new HashMap<>();
        for (GraphEdge graphEdge : this.edgeList) {
            if (!(graphEdge instanceof SequenceFlowBuilder)) {
                throw new IllegalStateException("Edge is not a instance of SequenceFlow");
            }
            SequenceFlowBuilder flowBuilder = (SequenceFlowBuilder) graphEdge;
            SequenceFlow sequenceFlow = flowBuilder.build();
            returnValue.addFlowElement(sequenceFlow);
            flowId2Flow.putIfAbsent(graphEdge.getGraphId(), sequenceFlow);
        }
        LinkedList<GraphVertex> vertexList = new LinkedList<>();
        for (GraphVertex vertex : this.vertexList) {
            if (vertex instanceof BaseBoundaryEventBuilder) {
                vertexList.add(vertex);
            } else {
                vertexList.addFirst(vertex);
            }
        }
        for (GraphVertex vertex : vertexList) {
            if (!(vertex instanceof BaseProcessNodeBuilder)) {
                throw new IllegalStateException("Vertex is not a instance of ProcessNode");
            }
            BaseProcessNodeBuilder<? extends FlowNode> nodeBuilder =
                    (BaseProcessNodeBuilder<? extends FlowNode>) vertex;
            FlowNode target = nodeBuilder.build();
            List<SequenceFlow> inFlowList = new LinkedList<>();
            for (GraphEdge inEdge : nodeBuilder.getInEdges()) {
                SequenceFlow sequenceFlow = flowId2Flow.get(inEdge.getGraphId());
                if (sequenceFlow == null) {
                    throw new NullPointerException("SequenceFlow is not found by id " + inEdge.getGraphId());
                }
                inFlowList.add(sequenceFlow);
            }
            List<SequenceFlow> outFlowList = new LinkedList<>();
            for (GraphEdge outEdge : nodeBuilder.getOutEdges()) {
                SequenceFlow sequenceFlow = flowId2Flow.get(outEdge.getGraphId());
                if (sequenceFlow == null) {
                    throw new NullPointerException("SequenceFlow is not found by id " + outEdge.getGraphId());
                }
                outFlowList.add(sequenceFlow);
            }
            if (!inFlowList.isEmpty()) {
                target.setIncomingFlows(inFlowList);
            }
            if (!outFlowList.isEmpty()) {
                target.setOutgoingFlows(outFlowList);
            }
            returnValue.addFlowElement(target);
        }
        init(returnValue);
        return returnValue;
    }

    private void init(Process process) {
        process.setId(FlowIdGenerators.uniqueStringIdGenerator().generateId());
        process.setName(name);
        process.setExecutable(executable);
        enableExecutionListeners(process);
    }

    protected void enableExecutionListeners(@NonNull Process target) {
        List<FlowableListener> listeners = new LinkedList<>();
        String[] eventNames = new String[] {
                FlowConstants.EXECUTION_START_EVENT_NAME,
                FlowConstants.EXECUTION_END_EVENT_NAME,
                FlowConstants.EXECUTION_TAKE_EVENT_NAME
        };
        for (String listenerClass : listenerClassNames) {
            for (String eventName : eventNames) {
                FlowableListener executionListener = new FlowableListener();
                executionListener.setImplementation(listenerClass);
                executionListener.setImplementationType("class");
                executionListener.setEvent(eventName);
                listeners.add(executionListener);
            }
        }
        target.setExecutionListeners(listeners);
    }

}
