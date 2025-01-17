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

import static com.oceanbase.odc.service.monitor.DefaultMeterName.SCHEDULE_FAILED_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.SCHEDULE_INTERRUPTED_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.SCHEDULE_START_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.SCHEDULE_SUCCESS_COUNT;
import static com.oceanbase.odc.service.monitor.DefaultMeterName.SCHEDULE_TASK_DURATION;

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.UnableToInterruptJobException;

import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.metadb.schedule.ScheduleTaskRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.monitor.DefaultMeterName;
import com.oceanbase.odc.service.monitor.MeterKey;
import com.oceanbase.odc.service.monitor.MeterManager;
import com.oceanbase.odc.service.schedule.job.DataArchiveDeleteJob;
import com.oceanbase.odc.service.schedule.job.DataArchiveJob;
import com.oceanbase.odc.service.schedule.job.DataArchiveRollbackJob;
import com.oceanbase.odc.service.schedule.job.DataDeleteJob;
import com.oceanbase.odc.service.schedule.job.LoadDataJob;
import com.oceanbase.odc.service.schedule.job.LogicalDatabaseChangeJob;
import com.oceanbase.odc.service.schedule.job.OdcJob;
import com.oceanbase.odc.service.schedule.job.OnlineSchemaChangeCompleteJob;
import com.oceanbase.odc.service.schedule.job.PartitionPlanJob;
import com.oceanbase.odc.service.schedule.job.SqlPlanJob;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;

import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author：tinker
 * @Date: 2022/12/27 21:26
 * @Descripition:
 */
@Slf4j
public abstract class AbstractQuartzJob implements InterruptableJob {

    private OdcJob odcJob;

    private JobKey jobKey;

    private MeterManager meterManager;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        log.info("Start to run new job,jobKey={}", context.getJobDetail().getKey());
        try {
            this.jobKey = context.getJobDetail().getKey();
            this.meterManager = SpringContextUtil.getBean(MeterManager.class);
            odcJob = getOdcJob(context);
            odcJob.before(context);
            sendStartMetric();
            run(context);
            sendEndMetric();
        } catch (Exception e) {
            sendFailedMetric();
            try {
                log.info("Start to update schedule task status to failed,jobKey={}", jobKey);
                ScheduleTaskRepository taskRepository = SpringContextUtil.getBean(ScheduleTaskRepository.class);
                ScheduleTaskEntity taskEntity = (ScheduleTaskEntity) context.getResult();
                if (taskEntity != null && taskEntity.getId() != null) {
                    taskRepository.updateStatusById(taskEntity.getId(), TaskStatus.FAILED);
                }
            } catch (Exception innerException) {
                log.warn("Update schedule task status failed.", innerException);
            }
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
        ScheduleTaskType taskType = ScheduleTaskType.valueOf(key.getGroup());
        switch (taskType) {
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
            case LOAD_DATA:
                return new LoadDataJob();
            case LOGICAL_DATABASE_CHANGE:
                return new LogicalDatabaseChangeJob();
            default:
                throw new UnsupportedException();
        }
    }


    @Override
    public void interrupt() throws UnableToInterruptJobException {
        odcJob.interrupt();
        sendInterruptMetric();
    }

    private void sendInterruptMetric() {
        meterManager.incrementCounter(getMeterKey(SCHEDULE_INTERRUPTED_COUNT, this.jobKey.getGroup()));
        meterManager.recordTimerSample(this.jobKey.getGroup(),
                getMeterKey(SCHEDULE_TASK_DURATION, this.jobKey.getGroup()));
    }

    private void sendStartMetric() {
        meterManager.incrementCounter(getMeterKey(SCHEDULE_START_COUNT, this.jobKey.getGroup()));
        meterManager.startTimerSample(this.jobKey.getGroup(),
                getMeterKey(SCHEDULE_TASK_DURATION, this.jobKey.getGroup()));
    }

    private void sendEndMetric() {
        meterManager.recordTimerSample(this.jobKey.getGroup(),
                getMeterKey(SCHEDULE_TASK_DURATION, this.jobKey.getGroup()));
        meterManager.incrementCounter(getMeterKey(SCHEDULE_SUCCESS_COUNT, this.jobKey.getGroup()));
    }

    private void sendFailedMetric() {
        meterManager.recordTimerSample(this.jobKey.getGroup(),
                getMeterKey(SCHEDULE_TASK_DURATION, this.jobKey.getGroup()));
        meterManager.incrementCounter(getMeterKey(SCHEDULE_FAILED_COUNT, this.jobKey.getGroup()));
    }


    public MeterKey getMeterKey(DefaultMeterName meterName, String taskType) {
        return MeterKey.ofMeter(meterName, Tag.of("taskType", taskType));
    }
}
