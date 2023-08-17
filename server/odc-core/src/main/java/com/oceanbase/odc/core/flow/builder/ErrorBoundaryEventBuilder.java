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

import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.ErrorEventDefinition;

import lombok.Getter;
import lombok.NonNull;

/**
 * Error event process node
 *
 * @author yh263208
 * @date 2022-01-19 15:07
 * @since ODC_release_3.3.0
 * @see BaseProcessNodeBuilder
 */
public class ErrorBoundaryEventBuilder extends BaseBoundaryEventBuilder {

    @Getter
    private final String errorCode;

    public ErrorBoundaryEventBuilder(@NonNull String name, @NonNull String attachedToRefId, @NonNull String errorCode) {
        super(name, attachedToRefId);
        this.errorCode = errorCode;
    }

    @Override
    public BoundaryEvent build() {
        BoundaryEvent returnValue = new BoundaryEvent();
        init(returnValue);
        return returnValue;
    }

    @Override
    protected void init(@NonNull BoundaryEvent event) {
        super.init(event);
        ErrorEventDefinition errorEventDefinition = new ErrorEventDefinition();
        errorEventDefinition.setErrorCode(errorCode);
        event.setCancelActivity(cancelActivity);
        event.addEventDefinition(errorEventDefinition);
    }

}
