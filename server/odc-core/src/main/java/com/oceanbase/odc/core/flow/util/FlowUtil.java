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
package com.oceanbase.odc.core.flow.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

import com.oceanbase.odc.core.flow.builder.FlowableProcessBuilder;

import lombok.NonNull;

/**
 * Util for Flow module
 *
 * @author yh263208
 * @date 2022-02-23 14:40
 * @since ODC_release_3.3.0
 */
public class FlowUtil {

    public static String convertToXml(@NonNull FlowableProcessBuilder builder) {
        return convertToXml(builder.build());
    }

    public static String convertToXml(@NonNull Process process) {
        BpmnModel bpmnModel = new BpmnModel();
        bpmnModel.addProcess(process);

        BpmnXMLConverter converter = new BpmnXMLConverter();
        return new String(converter.convertToXML(bpmnModel));
    }

    @SuppressWarnings("all")
    public static <T extends EventDefinition> List<T> getBoundaryEventDefinitions(@NonNull String processDefinitionId,
            @NonNull String activityId, @NonNull Class<T> eventClass) {
        Process process = ProcessDefinitionUtil.getProcess(processDefinitionId);
        FlowElement element = process.getFlowElement(activityId);
        if (!(element instanceof Activity)) {
            return Collections.emptyList();
        }
        List<BoundaryEvent> boundaryEvents = ((Activity) element).getBoundaryEvents();
        if (CollectionUtils.isEmpty(boundaryEvents)) {
            return Collections.emptyList();
        }
        List<T> returnVal = new LinkedList<>();
        for (BoundaryEvent boundaryEvent : boundaryEvents) {
            List<EventDefinition> eventDefinitions = boundaryEvent.getEventDefinitions();
            for (EventDefinition definition : eventDefinitions) {
                if (eventClass.equals(definition.getClass())) {
                    returnVal.add((T) definition);
                }
            }
        }
        return returnVal;
    }

}
