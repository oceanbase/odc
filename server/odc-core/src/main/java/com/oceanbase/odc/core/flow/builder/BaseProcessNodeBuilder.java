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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.Validate;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.FormProperty;

import com.oceanbase.odc.core.flow.BaseExecutionListener;
import com.oceanbase.odc.core.flow.ProcessElementBuilder;
import com.oceanbase.odc.core.flow.graph.GraphVertex;
import com.oceanbase.odc.core.flow.util.FlowConstants;
import com.oceanbase.odc.core.flow.util.FlowIdGenerators;

import lombok.NonNull;

/**
 * Refer to {@link org.flowable.bpmn.model.UserTask} {@link org.flowable.bpmn.model.ServiceTask}
 *
 * @author yh263208
 * @date 2022-01-18 20:14
 * @since ODC_release_3.3.0
 */
public abstract class BaseProcessNodeBuilder<T extends FlowNode> extends GraphVertex
        implements ProcessElementBuilder<T> {

    private final Map<String, FormProperty> propertyId2Property = new HashMap<>();
    private boolean asynchronous = false;
    private final Set<String> listenerClassNames = new HashSet<>();

    public BaseProcessNodeBuilder(@NonNull String name) {
        super(FlowIdGenerators.uniqueStringIdGenerator().generateId(), name);
    }

    public BaseProcessNodeBuilder<T> addExecutionListener(
            @NonNull Class<? extends BaseExecutionListener> listenerClass) {
        this.listenerClassNames.add(listenerClass.getName());
        return this;
    }

    public BaseProcessNodeBuilder<T> addFormProperty(@NonNull FormProperty formProperty) {
        Validate.notNull(formProperty.getId(), "FormProperty's id can not be null");
        FormProperty property = propertyId2Property.get(formProperty.getId());
        if (property != null) {
            throw new IllegalStateException("Property with id " + property.getId() + " has been exist");
        }
        propertyId2Property.putIfAbsent(formProperty.getId(), formProperty);
        return this;
    }

    public BaseProcessNodeBuilder<T> setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
        return this;
    }

    protected void forEachFormProperty(@NonNull Consumer<FormProperty> consumer) {
        this.propertyId2Property.values().forEach(consumer);
    }

    protected void enableExecutionListeners(@NonNull T target) {
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

    protected void init(@NonNull T target) {
        target.setName(getName());
        target.setId(getGraphId());
        target.setAsynchronous(asynchronous);
        enableExecutionListeners(target);
    }

    /**
     * Each process node may has sub process node
     *
     * @return list of {@link BaseProcessNodeBuilder}
     */
    public List<BaseProcessNodeBuilder<?>> getSubProcessNodeBuilders() {
        return Collections.emptyList();
    }

}
