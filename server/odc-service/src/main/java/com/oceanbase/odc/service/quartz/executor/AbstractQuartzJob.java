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
package com.oceanbase.odc.service.quartz.executor;

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.UnableToInterruptJobException;

import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.schedule.job.DataArchiveDeleteJob;
import com.oceanbase.odc.service.schedule.job.DataArchiveJob;
import com.oceanbase.odc.service.schedule.job.DataArchiveRollbackJob;
import com.oceanbase.odc.service.schedule.job.DataDeleteJob;
import com.oceanbase.odc.service.schedule.job.OdcJob;
import com.oceanbase.odc.service.schedule.job.OnlineSchemaChangeCompleteJob;
import com.oceanbase.odc.service.schedule.job.PartitionPlanJob;
import com.oceanbase.odc.service.schedule.job.SqlPlanJob;
import com.oceanbase.odc.service.schedule.model.JobType;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2022/12/27 21:26
 * @Descripition:
 */
@Slf4j
public abstract class AbstractQuartzJob implements InterruptableJob {

    private OdcJob odcJob;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        log.info("Start to run new job,jobKey={}", context.getJobDetail().getKey());
        try {
            odcJob = getOdcJob(context);
            odcJob.before(context);
            run(context);
        } catch (Exception e) {
            log.warn("Job execute failed,job key={},fire time={}.",
                    context.getJobDetail().getKey(), context.getFireTime(), e);
        } finally {
            odcJob.after(context);
        }

        log.info("Job done,jobKey={}", context.getJobDetail().getKey());

    }

    public void run(JobExecutionContext context) {
        odcJob.execute(context);
    }

    public OdcJob getOdcJob(JobExecutionContext context) {
        JobKey key = context.getJobDetail().getKey();
        JobType jobType = JobType.valueOf(key.getGroup());
        switch (jobType) {
            case SQL_PLAN:
                return new SqlPlanJob();
            case DATA_ARCHIVE:
                return new DataArchiveJob();
            case DATA_ARCHIVE_DELETE:
                return new DataArchiveDeleteJob();
            case DATA_ARCHIVE_ROLLBACK:
                return new DataArchiveRollbackJob();
            case DATA_DELETE:
                return new DataDeleteJob();
            case ONLINE_SCHEMA_CHANGE_COMPLETE:
                return new OnlineSchemaChangeCompleteJob();
            case PARTITION_PLAN:
                return new PartitionPlanJob();
            default:
                throw new UnsupportedException();
        }
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        odcJob.interrupt();
    }
}
