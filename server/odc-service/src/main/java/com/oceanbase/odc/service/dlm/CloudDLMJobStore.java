/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.dlm;

import java.sql.SQLException;
import java.util.List;

import com.oceanbase.tools.migrator.common.dto.JobStatistic;
import com.oceanbase.tools.migrator.common.dto.TableSizeInfo;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;
import com.oceanbase.tools.migrator.common.exception.JobException;
import com.oceanbase.tools.migrator.common.exception.JobSqlException;
import com.oceanbase.tools.migrator.common.exception.TaskGeneratorNotFoundException;
import com.oceanbase.tools.migrator.core.IJobStore;
import com.oceanbase.tools.migrator.core.meta.JobMeta;
import com.oceanbase.tools.migrator.core.meta.TaskMeta;

/**
 * @Author：tinker
 * @Date: 2024/1/24 19:57
 * @Descripition:
 */
public class CloudDLMJobStore implements IJobStore {
    @Override
    public TaskGenerator getTaskGenerator(String s, String s1) throws TaskGeneratorNotFoundException, SQLException {
        return null;
    }

    @Override
    public void storeTaskGenerator(TaskGenerator taskGenerator) throws SQLException {

    }

    @Override
    public void bindGeneratorToJob(String s, TaskGenerator taskGenerator) throws SQLException {

    }

    @Override
    public JobStatistic getJobStatistic(String s) throws JobException {
        return null;
    }

    @Override
    public void storeJobStatistic(JobMeta jobMeta) throws JobSqlException {

    }

    @Override
    public List<TaskMeta> getTaskMeta(JobMeta jobMeta) throws SQLException {
        return null;
    }

    @Override
    public void storeTaskMeta(TaskMeta taskMeta) throws SQLException {

    }

    @Override
    public Long getAbnormalTaskIndex(String s) throws JobSqlException {
        return null;
    }

    @Override
    public void updateTableSizeInfo(TableSizeInfo tableSizeInfo, long l) {

    }

    @Override
    public void updateLimiter(JobMeta jobMeta) throws SQLException {

    }
}
