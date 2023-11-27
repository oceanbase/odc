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

package com.oceanbase.odc.service.task.executor.sampletask;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.caller.JobContext;
import com.oceanbase.odc.service.task.executor.BaseTask;

import lombok.extern.slf4j.Slf4j;

/**
 * A sample task for testing. It will execute SQLs in the task parameters.
 * 
 * @author gaoda.xy
 * @date 2023/11/22 20:01
 */
@Slf4j
public class SampleTask extends BaseTask {

    private SampleTaskParameter parameter;

    public SampleTask(JobContext context) {
        super(context);
    }

    @Override
    protected void doStart() {
        this.status = TaskStatus.RUNNING;
        Verify.equals(TaskType.SAMPLE, context.getTaskType(), "taskType");
        this.parameter = JsonUtils.fromJson(context.getTaskParameters(), SampleTaskParameter.class);
        validateTaskParameter();
        ConnectionConfig connectionConfig = context.getConnectionConfigs().get(0);
        ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        try {
            JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
            for (String sql : this.parameter.getSqls()) {
                if (stopped) {
                    break;
                }
                jdbcOperations.execute(sql);
            }
            this.result = SampleTaskResult.success();
            this.status = TaskStatus.DONE;
        } catch (Exception e) {
            this.result = SampleTaskResult.fail();
            this.status = TaskStatus.FAILED;
        } finally {
            this.finished = true;
            session.expire();
        }
    }

    @Override
    protected void doStop() {
        this.stopped = true;
    }

    @Override
    protected void onFinished() {
        log.info("Task finished, id: {}, result: {}", context.getTaskId(), this.result);
    }

    @Override
    protected void onFailure(Exception e) {
        log.warn("Task failed, id: {}", context.getTaskId(), e);
    }

    @Override
    protected void onUpdateProgress() {
        log.info("Task progress updated, id: {}", context.getTaskId());
    }

    private void validateTaskParameter() {
        Verify.notNull(this.parameter, "parameter");
        Verify.notEmpty(this.parameter.getSqls(), "sql");
    }

}
