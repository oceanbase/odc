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
package com.oceanbase.odc.service.task.processor.terminate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.task.processor.ProcessorMatcher;

/**
 * processor ti handle terminate event
 * 
 * @author longpeng.zlp
 * @date 2024/10/10 11:26
 */
public interface TerminateProcessor extends ProcessorMatcher {
    /**
     * processor may want correct status by it's own logic
     * 
     * @param currentStatus
     * @return
     */
    default TaskStatus correctTaskStatus(ScheduleTask scheduleTask, TaskStatus currentStatus) {
        return currentStatus;
    }

    /**
     * handle input parameters and task
     */
    void process(ScheduleTask scheduleTask, JobEntity jobEntity);

    static TaskStatus correctTaskStatus(Collection<TerminateProcessor> terminateProcessors, String jobType,
            ScheduleTask scheduleTask, TaskStatus currentStatus) {
        List<TaskStatus> correctedStatus = new ArrayList<>();
        // return first matched
        for (TerminateProcessor processor : terminateProcessors) {
            if (processor.interested(jobType)) {
                correctedStatus.add(processor.correctTaskStatus(scheduleTask, currentStatus));
            }
        }
        if (correctedStatus.size() > 1) {
            throw new IllegalStateException(
                    "multi matcher found, not determinate final state, " + StringUtils.join(correctedStatus, ","));
        }
        return correctedStatus.size() == 0 ? currentStatus : correctedStatus.get(0);
    }
}
