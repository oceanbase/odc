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

package com.oceanbase.odc.service.task.dispatch;

import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.service.task.caller.JobCaller;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.caller.JobException;
import com.oceanbase.odc.service.task.caller.PodConfig;
import com.oceanbase.odc.service.task.config.JobConfiguration;
import com.oceanbase.odc.service.task.config.JobConfigurationHolder;
import com.oceanbase.odc.service.task.enums.TaskRunModeEnum;
import com.oceanbase.odc.service.task.schedule.JobCallerBuilder;

/**
 * Dispatch job to JobCaller immediately
 *
 * @author yaobin
 * @date 2023-11-20
 * @since 4.2.4
 */
public class ImmediateJobDispatcher implements JobDispatcher {

    @Override
    public void dispatch(JobContext context) throws JobException {

        JobCaller jobCaller = getJobCaller(JobConfigurationHolder.getJobConfiguration());
        jobCaller.start(context);
    }

    private JobCaller getJobCaller(JobConfiguration config) {
        if (config.getTaskFrameworkProperties().getRunMode() == TaskRunModeEnum.K8S) {
            String namespace = config.getTaskFrameworkProperties().getK8s().getNamespace();
            return JobCallerBuilder.buildK8sJobCaller(config.getK8sJobClient(), createDefaultPodConfig(namespace));
        }
        return JobCallerBuilder.buildJvmCaller();
    }


    private PodConfig createDefaultPodConfig(String namespace) {
        PodConfig podConfig = new PodConfig();
        // todo read odc version
        String imageName = SystemUtils.getEnvOrProperty("ODC_IMAGE");
        podConfig.setImage(imageName);
        podConfig.setNamespace(namespace);
        return podConfig;
    }


}
