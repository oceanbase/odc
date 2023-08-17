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

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;

import com.oceanbase.odc.core.flow.graph.GraphEdge;

import lombok.NonNull;
import lombok.Setter;

/**
 * {@link BaseBoundaryEventBuilder}
 *
 * @author yh263208
 * @date 2022-01-19 16:14
 * @since ODC_release_3.3.0
 * @see BaseProcessNodeBuilder
 */
abstract class BaseBoundaryEventBuilder extends BaseProcessNodeBuilder<BoundaryEvent> {
    @Setter
    protected boolean cancelActivity = true;
    private final String attachedToRefId;

    public BaseBoundaryEventBuilder(@NonNull String name, @NonNull String attachedToRefId) {
        super(name);
        this.attachedToRefId = attachedToRefId;
    }

    @Override
    public boolean addInEdge(@NonNull GraphEdge inEdge) {
        throw new UnsupportedOperationException("Can not add in edge for BoundaryEventBuilder");
    }

    @Override
    protected void init(@NonNull BoundaryEvent event) {
        super.init(event);
        Object value = getAttribute(BaseTaskBuilder.ATTACHED_REF_OBJ_ATTRI_NAME);
        if (!(value instanceof Activity)) {
            throw new IllegalStateException("AttachedToRef can not be null");
        }
        Activity target = (Activity) value;
        event.setAttachedToRef(target);
        event.setAttachedToRefId(attachedToRefId);
        target.getBoundaryEvents().add(event);
    }

}
