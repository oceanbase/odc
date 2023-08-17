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
package com.oceanbase.odc.core.flow;

import java.util.Objects;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;

import com.oceanbase.odc.core.flow.util.FlowConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link BaseExecutionListener}
 *
 * @author yh263208
 * @date 2022-01-19 10:57
 * @since ODC_release_3.3.0
 */
@Slf4j
public abstract class BaseExecutionListener implements ExecutionListener {
    /**
     * Method will be invoked when execution is started
     *
     * @param execution {@link DelegateExecution}
     */
    protected abstract void onExecutiuonStart(DelegateExecution execution);

    /**
     * Method will be invoked when execution is ended
     *
     * @param execution {@link DelegateExecution}
     */
    protected abstract void onExecutionEnd(DelegateExecution execution);

    /**
     * Method will be invoked when execution is ended
     *
     * @param execution {@link DelegateExecution}
     */
    protected abstract void onExecutionTaken(DelegateExecution execution);

    @Override
    public void notify(DelegateExecution execution) {
        String eventName = execution.getEventName();
        if (Objects.equals(eventName, FlowConstants.EXECUTION_END_EVENT_NAME)) {
            try {
                onExecutionEnd(execution);
            } catch (Exception e) {
                log.warn("Failed to execute listener method, eventName={}, executionId={}, activityId={}",
                        eventName, execution.getId(), execution.getCurrentActivityId(), e);
            }
        } else if (Objects.equals(eventName, FlowConstants.EXECUTION_START_EVENT_NAME)) {
            try {
                onExecutiuonStart(execution);
            } catch (Exception e) {
                log.warn("Failed to execute listener method, eventName={}, executionId={}, activityId={}",
                        eventName, execution.getId(), execution.getCurrentActivityId(), e);
            }
        } else if (Objects.equals(eventName, FlowConstants.EXECUTION_TAKE_EVENT_NAME)) {
            try {
                onExecutionTaken(execution);
            } catch (Exception e) {
                log.warn("Failed to execute listener method, eventName={}, executionId={}, activityId={}",
                        eventName, execution.getId(), execution.getCurrentActivityId(), e);
            }
        } else {
            log.warn("Invalid event name, eventName={}, executionId={}, activityId={}", eventName, execution.getId(),
                    execution.getCurrentActivityId());
        }
    }

}
