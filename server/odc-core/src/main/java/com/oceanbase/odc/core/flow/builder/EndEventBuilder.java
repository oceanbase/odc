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

import org.flowable.bpmn.model.EndEvent;

import com.oceanbase.odc.core.flow.graph.GraphEdge;

import lombok.NonNull;

/**
 * Refer to {@link org.flowable.bpmn.model.EndEvent}
 *
 * @author yh263208
 * @date 2022-01-18 17:36
 * @since ODC_release_3.3.0
 */
public class EndEventBuilder extends BaseProcessNodeBuilder<EndEvent> {

    public EndEventBuilder() {
        super("End Node");
    }

    @Override
    public boolean addOutEdge(@NonNull GraphEdge outEdge) {
        throw new UnsupportedOperationException("Can not add out edge for End Node");
    }

    @Override
    public EndEvent build() {
        EndEvent endEvent = new EndEvent();
        init(endEvent);
        return endEvent;
    }

}
