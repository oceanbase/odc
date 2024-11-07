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
package com.oceanbase.odc.agent.runtime;

import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.base.BaseTask;
import com.oceanbase.odc.service.task.exception.TaskRuntimeException;

/**
 * @author gaoda.xy
 * @date 2023/11/24 11:01
 */
public class TaskFactory {

    public static BaseTask<?> create(String jobClass) {
        try {
            Class<?> c = Class.forName(jobClass);
            if (!Task.class.isAssignableFrom(c)) {
                throw new TaskRuntimeException("Job class is not implements Task. name={}" + jobClass);
            }
            return (BaseTask<?>) c.newInstance();
        } catch (Exception e) {
            throw new TaskRuntimeException(e);
        }
    }

}
