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

import java.io.Serializable;
import java.util.Map;

import org.springframework.jdbc.core.JdbcOperations;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionConstants;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.session.factory.DefaultConnectSessionFactory;
import com.oceanbase.odc.service.task.constants.JobDataMapConstants;
import com.oceanbase.odc.service.task.enums.JobStatus;
import com.oceanbase.odc.service.task.executor.task.BaseTask;

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

    private int executedSqlCount = 0;

    private int totalSqlCount = 0;

    private FlowTaskResult result;

    @Override
    protected void onInit() {

    }

    @Override
    protected void onStart() {
        updateStatus(JobStatus.RUNNING);
        Map<String, String> dataMap = getJobContext().getJobParameters();

        this.parameter =
                JsonUtils.fromJson(dataMap.get(JobDataMapConstants.META_DB_TASK_PARAMETER), SampleTaskParameter.class);
        validateTaskParameter();
        this.totalSqlCount = this.parameter.getSqls().size();
        ConnectionConfig connectionConfig =
                JsonUtils.fromJson(dataMap.get(JobDataMapConstants.CONNECTION_CONFIG), ConnectionConfig.class);
        connectionConfig.setId(1L); // Set connection id to 1 for testing.
        connectionConfig.setDefaultSchema(this.parameter.getDefaultSchema());
        ConnectionSession session = new DefaultConnectSessionFactory(connectionConfig).generateSession();
        try {
            JdbcOperations jdbcOperations = session.getSyncJdbcExecutor(ConnectionSessionConstants.BACKEND_DS_KEY);
            for (String sql : this.parameter.getSqls()) {
                if (isCanceled()) {
                    break;
                }
                jdbcOperations.execute(sql);
                executedSqlCount++;
                Thread.sleep(500); // Simulate long execution time of SQL.
            }
            this.result = SampleTaskResult.success();
            updateStatus(JobStatus.DONE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            session.expire();
        }
    }

    @Override
    protected void onStop() {

    }

    @Override
    protected void onFail(Throwable e) {
        this.result = SampleTaskResult.fail();
    }

    private void validateTaskParameter() {
        Verify.notNull(this.parameter, "parameter");
        Verify.notEmpty(this.parameter.getSqls(), "sql");
    }

    @Override
    public double getProgress() {
        return executedSqlCount * 100.0 / totalSqlCount;
    }

    @Override
    public Serializable getTaskResult() {
        return this.result;
    }

}
