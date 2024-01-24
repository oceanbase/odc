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
package com.oceanbase.odc.service.task.caller;

import com.oceanbase.odc.service.task.constants.JobConstants;
import com.oceanbase.odc.service.task.constants.JobEnvKeyConstants;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;

/**
 * @author yaobin
 * @date 2023-11-29
 * @since 4.2.4
 */
public class JobCallerBuilder {

    public static JobCaller buildProcessCaller(JobContext context) {
        ProcessConfig config = new ProcessConfig();
        config.setEnvironments(new JobEnvBuilder().buildMap(context, TaskRunModeEnum.PROCESS));
        return new ProcessJobCaller(config);
    }

    public static JobCaller buildK8sJobCaller(K8sJobClient k8sJobClient, PodConfig podConfig, JobContext context) {

        PodParam podParam = podConfig.getPodParam();
        podParam.setEnvironments(new JobEnvBuilder().buildMap(context, TaskRunModeEnum.K8S));
        podParam.getEnvironments().putIfAbsent(JobEnvKeyConstants.ODC_LOG_DIRECTORY,
            JobConstants.TASK_EXECUTOR_DEFAULT_MOUNT_PATH);
        return new K8sJobCaller(k8sJobClient, podConfig);
    }
}
