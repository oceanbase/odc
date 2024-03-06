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

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;

import com.oceanbase.odc.core.shared.exception.InternalServerError;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom Service task
 *
 * @author yh263208
 * @date 2022-01-19 17:23
 * @since ODC_release_3.3.0
 */
@Slf4j
public abstract class BaseFlowableDelegate implements JavaDelegate {
    /**
     * custom method logic
     *
     * @param execution execution context
     */
    protected abstract void run(DelegateExecution execution) throws Exception;

    @Override
    public void execute(DelegateExecution execution) {
        try {
            run(execution);
        } catch (Exception exception) {
            throw new InternalServerError("Execute task occur error.", exception);
        }
    }

}
