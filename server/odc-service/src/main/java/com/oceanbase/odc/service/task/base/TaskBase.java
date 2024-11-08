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
package com.oceanbase.odc.service.task.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oceanbase.odc.service.task.Task;
import com.oceanbase.odc.service.task.TaskContext;
import com.oceanbase.odc.service.task.caller.DefaultJobContext;
import com.oceanbase.odc.service.task.caller.JobContext;

import lombok.extern.slf4j.Slf4j;

/**
 * base task for implement
 * 
 * @author longpeng.zlp
 * @date 2024/11/8 11:10
 */
@Slf4j
public abstract class TaskBase<RESULT> implements Task<RESULT> {
    protected TaskContext context;
    protected DefaultJobContext jobContext;

    public void init(TaskContext taskContext) throws Exception {
        this.context = taskContext;
        jobContext = copyJobContext(taskContext);
        log.info("Start task, id={}.", jobContext.getJobIdentity().getId());
        log.info("Init task parameters success, id={}.", jobContext.getJobIdentity().getId());
        doInit(jobContext);
    }

    protected abstract void doInit(JobContext context) throws Exception;

    public JobContext getJobContext() {
        return jobContext;
    }

    // deep copy job context
    protected DefaultJobContext copyJobContext(TaskContext taskContext) {
        DefaultJobContext ret = new DefaultJobContext();
        JobContext src = taskContext.getJobContext();
        ret.setJobIdentity(src.getJobIdentity());
        if (null != src.getJobProperties()) {
            ret.setJobProperties(new HashMap<>(src.getJobProperties()));
        }
        if (null != src.getJobParameters()) {
            ret.setJobParameters(Collections.unmodifiableMap(new HashMap<>(src.getJobParameters())));
        }
        ret.setJobClass(src.getJobClass());
        if (null != src.getHostUrls()) {
            ret.setHostUrls(new ArrayList<>(src.getHostUrls()));
        }
        return ret;
    }

    protected Long getJobId() {
        return getJobContext().getJobIdentity().getId();
    }

    @Override
    public boolean modify(Map<String, String> jobParameters) {
        if (Objects.isNull(jobParameters) || jobParameters.isEmpty()) {
            log.warn("Job parameter cannot be null, id={}", getJobId());
            return false;
        }
        jobContext.setJobParameters(Collections.unmodifiableMap(jobParameters));
        return true;
    }

}
