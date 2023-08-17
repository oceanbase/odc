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

import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;

import lombok.NonNull;

/**
 * Refer to {@link IntermediateCatchEvent}
 *
 * @author yh263208
 * @date 2022-01-19 16:47
 * @since ODC_release_3.3.0
 * @see BaseProcessNodeBuilder
 */
public class SignalCatchEventBuilder extends BaseProcessNodeBuilder<IntermediateCatchEvent> {

    private final String signalName;

    public SignalCatchEventBuilder(@NonNull String name, @NonNull String signalName) {
        super(name);
        this.signalName = signalName;
    }

    @Override
    public IntermediateCatchEvent build() {
        IntermediateCatchEvent returnVal = new IntermediateCatchEvent();
        init(returnVal);
        return returnVal;
    }

    @Override
    protected void init(@NonNull IntermediateCatchEvent event) {
        super.init(event);
        Signal signal = new Signal();
        signal.setName(signalName);
        signal.setId(signalName);
        SignalEventDefinition definition = new SignalEventDefinition();
        definition.setSignalRef(signalName);
        event.addEventDefinition(definition);
    }

}
