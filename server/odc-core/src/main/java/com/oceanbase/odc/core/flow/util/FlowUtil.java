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
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;

import com.oceanbase.odc.core.flow.builder.FlowableProcessBuilder;
import com.oceanbase.odc.core.flow.exception.BaseFlowException;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Util for Flow module
 *
 * @author yh263208
 * @date 2022-02-23 14:40
 * @since ODC_release_3.3.0
 */
@Slf4j
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


    public static boolean isApprovalPassed(String processDefinitionId, String currentActivityId, Throwable exception) {
        if (exception == null) {
            return true;
        }
        try {
            handleException(processDefinitionId, currentActivityId, exception);
            return true;
            // approve
        } catch (BpmnError e) {
            log.warn("Found error boundary is defined to handle events, processInstanceId={}, activityId={}",
                processDefinitionId, currentActivityId);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static void handleException(String processDefinitionId, String currentActivityId, Throwable exception) {
        List<ErrorEventDefinition> defs =
            FlowUtil.getBoundaryEventDefinitions(processDefinitionId, currentActivityId,
                ErrorEventDefinition.class);
        if (defs.isEmpty()) {
            log.warn("No error boundary is defined to handle events, processInstanceId={}, activityId={}",
                processDefinitionId, currentActivityId);
        } else if (exception instanceof BaseFlowException) {
            BaseFlowException flowException = (BaseFlowException) exception;
            String targetErrorCode = flowException.getErrorCode().code();
            for (ErrorEventDefinition eventDefinition : defs) {
                String acceptErrorCode = eventDefinition.getErrorCode();
                if (Objects.equals(acceptErrorCode, targetErrorCode)) {
                    throw new BpmnError(targetErrorCode);
                }
            }
            log.warn("Exception has no error boundary event, pId={}, activityId={}, acceptErrorCodes={}",
                processDefinitionId, currentActivityId,
                defs.stream().map(ErrorEventDefinition::getErrorCode).collect(Collectors.toList()));
        } else {
            log.warn("Exception has to be an instance of FlowException, pId={}, activityId={}, acceptErrorCodes={}",
                processDefinitionId, currentActivityId,
                defs.stream().map(ErrorEventDefinition::getErrorCode).collect(Collectors.toList()));
        }
    }


}
