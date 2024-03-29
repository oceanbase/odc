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
package com.oceanbase.odc.service.dispatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.metadb.task.JobEntity;
import com.oceanbase.odc.metadb.task.TaskEntity;
import com.oceanbase.odc.service.common.model.HostProperties;
import com.oceanbase.odc.service.task.config.TaskFrameworkEnabledProperties;
import com.oceanbase.odc.service.task.model.ExecutorInfo;
import com.oceanbase.odc.service.task.service.TaskFrameworkService;

import lombok.NonNull;

/**
 * {@link WebTaskDispatchChecker}
 *
 * @author yh263208
 * @date 2022-03-29 16:37
 * @since ODC_release_3.3.0
 * @see TaskDispatchChecker
 */
@Component
@Profile("alipay")
public class WebTaskDispatchChecker implements TaskDispatchChecker {

    @Autowired
    private HostProperties hostProperties;
    @Autowired
    private TaskFrameworkService taskFrameworkService;
    @Autowired
    private TaskFrameworkEnabledProperties taskFrameworkProperties;
    @Autowired
    private JobDispatchChecker jobDispatchChecker;

    @Override
    public boolean isThisMachine(@NonNull ExecutorInfo info) {
        ExecutorInfo current = new ExecutorInfo(hostProperties);
        return current.equals(info);
    }

    @Override
    public boolean isTaskEntityOnThisMachine(@NonNull TaskEntity taskEntity) {
        if (taskFrameworkProperties.isEnabled() && taskEntity.getJobId() != null) {
            JobEntity jobEntity = taskFrameworkService.find(taskEntity.getJobId());
            if (jobEntity == null) {
                return true;
            }
            return jobDispatchChecker.isExecutorOnThisMachine(jobEntity);
        }
        ExecutorInfo executorInfo = JsonUtils.fromJson(taskEntity.getExecutor(), ExecutorInfo.class);
        if (executorInfo == null) {
            return true;
        }
        return isThisMachine(executorInfo);
    }

    @Override
    public boolean isTaskEntitySubmitOnThisMachine(@NonNull TaskEntity taskEntity) {
        ExecutorInfo submitterInfo = JsonUtils.fromJson(taskEntity.getSubmitter(), ExecutorInfo.class);
        if (submitterInfo == null) {
            return true;
        }
        return isThisMachine(submitterInfo);
    }

}
