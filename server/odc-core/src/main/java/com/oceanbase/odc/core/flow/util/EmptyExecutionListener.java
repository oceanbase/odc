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

import org.flowable.engine.delegate.DelegateExecution;

import com.oceanbase.odc.core.flow.BaseExecutionListener;

import lombok.extern.slf4j.Slf4j;

/**
 * Empty {@link BaseExecutionListener}
 *
 * @author yh263208
 * @date 2022-02-22 20:05
 * @since ODC_release_3.3.0
 * @see BaseExecutionListener
 */
@Slf4j
public class EmptyExecutionListener extends BaseExecutionListener {

    @Override
    protected void onExecutiuonStart(DelegateExecution execution) {
        if (log.isDebugEnabled()) {
            log.debug("Begin execution, activityId={}, processInstanceId={}", execution.getCurrentActivityId(),
                    execution.getProcessInstanceId());
            execution.getCurrentFlowableListener().getEvent();
            execution.getCurrentFlowableListener();
        }
    }

    @Override
    protected void onExecutionEnd(DelegateExecution execution) {
        if (log.isDebugEnabled()) {
            log.debug("End execution, activityId={}, processInstanceId={}", execution.getCurrentActivityId(),
                    execution.getProcessInstanceId());
        }
    }

    @Override
    protected void onExecutionTaken(DelegateExecution execution) {
        if (log.isDebugEnabled()) {
            log.debug("Taken execution, activityId={}, processInstanceId={}", execution.getCurrentActivityId(),
                    execution.getProcessInstanceId());
        }
    }

}
