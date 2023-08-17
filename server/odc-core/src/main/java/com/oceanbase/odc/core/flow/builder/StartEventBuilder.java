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

import java.util.LinkedList;
import java.util.List;

import org.flowable.bpmn.model.FormProperty;
import org.flowable.bpmn.model.StartEvent;

import com.oceanbase.odc.core.flow.graph.GraphEdge;
import com.oceanbase.odc.core.flow.graph.GraphVertex;

import lombok.NonNull;

/**
 * Refer to {@link org.flowable.bpmn.model.StartEvent}
 *
 * @author yh263208
 * @date 2022-01-18 17:28
 * @since ODC_release_3.3.0
 * @see GraphVertex
 */
public class StartEventBuilder extends BaseProcessNodeBuilder<StartEvent> {

    public StartEventBuilder() {
        super("Start Node");
    }

    @Override
    public boolean addInEdge(@NonNull GraphEdge inEdge) {
        throw new UnsupportedOperationException("Can not add in edge for Start Node");
    }

    @Override
    public StartEvent build() {
        StartEvent startEvent = new StartEvent();
        init(startEvent);
        return startEvent;
    }

    @Override
    protected void init(@NonNull StartEvent startEvent) {
        super.init(startEvent);
        List<FormProperty> properties = new LinkedList<>();
        forEachFormProperty(properties::add);
        if (!properties.isEmpty()) {
            startEvent.setFormProperties(properties);
        }
    }

}
