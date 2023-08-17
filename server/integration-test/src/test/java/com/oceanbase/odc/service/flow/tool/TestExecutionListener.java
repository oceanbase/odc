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
package com.oceanbase.odc.service.flow.tool;

import org.flowable.engine.delegate.DelegateExecution;

import com.oceanbase.odc.core.flow.BaseExecutionListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestExecutionListener extends BaseExecutionListener {

    @Override
    protected void onExecutiuonStart(DelegateExecution execution) {
        log.info("Execution start, currentActivityId={}", execution.getCurrentActivityId());
    }

    @Override
    protected void onExecutionEnd(DelegateExecution execution) {
        log.info("Execution end, currentActivityId={}", execution.getCurrentActivityId());

    }

    @Override
    protected void onExecutionTaken(DelegateExecution execution) {
        log.info("Execution taken, currentActivityId={}", execution.getCurrentActivityId());
    }

}
