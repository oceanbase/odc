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
package com.oceanbase.odc.service.flow.task.util;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.BpmnError;

import com.oceanbase.odc.core.flow.exception.BaseFlowException;
import com.oceanbase.odc.core.flow.util.FlowUtil;
import com.oceanbase.odc.service.common.util.SpringContextUtil;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author yaobin
 * @date 2024-02-29
 * @since 4.2.4
 */
@Slf4j
public class FlowApprovalUtil {

    public static boolean isApprovalPassed(String processDefinitionId, String currentActivityId, Throwable exception) {
        if (exception == null) {
            return true;
        }
        try {
            handleException(processDefinitionId, currentActivityId, exception);
            return true;
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
                SpringContextUtil.getBean(ProcessEngineConfiguration.class).getCommandExecutor()
                        .execute(context -> FlowUtil.getBoundaryEventDefinitions(processDefinitionId,
                                currentActivityId, ErrorEventDefinition.class));

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
