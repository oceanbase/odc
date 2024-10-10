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
package com.oceanbase.odc.service.quartz.util;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.DataDeleteParameters;
import com.oceanbase.odc.service.flow.task.model.DatabaseChangeParameters;
import com.oceanbase.odc.service.loaddata.model.LoadDataParameters;
import com.oceanbase.odc.service.schedule.model.DataArchiveClearParameters;
import com.oceanbase.odc.service.schedule.model.DataArchiveRollbackParameters;
import com.oceanbase.odc.service.schedule.model.LogicalDatabaseChangeParameters;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskParameters;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskType;
import com.oceanbase.odc.service.sqlplan.model.SqlPlanParameters;

/**
 * @Author：tinker
 * @Date: 2023/6/25 14:23
 * @Descripition:
 */
public class ScheduleTaskUtils {

    public static final String id = "ODC_SCHEDULE_TASK_OERPATION_ID";
    private static final String SCHEDULE_TASK_ID = "ODC_SCHEDULE_TASK_ID";
    private static final String SCHEDULE_TASK_PARAMETERS = "ODC_SCHEDULE_TASK_PARAMETERS";

    public static JobDataMap buildTriggerDataMap(Long taskId) {
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(id, taskId);
        return dataMap;
    }

    public static Long getTargetTaskId(JobExecutionContext context) {
        JobDataMap triggerDataMap = context.getTrigger().getJobDataMap();
        if (triggerDataMap != null && !triggerDataMap.isEmpty()) {
            return Long.parseLong(triggerDataMap.get(id).toString());
        }
        return null;
    }

    public static Long getScheduleId(JobExecutionContext context) {
        String name = context.getJobDetail().getKey().getName();
        return Long.parseLong(name);
    }

    public static ScheduleTaskType getScheduleTaskType(JobExecutionContext context) {
        return ScheduleTaskType.valueOf(context.getJobDetail().getKey().getGroup());
    }

    public static String getJobName(JobExecutionContext context) {
        return context.getJobDetail().getKey().getName();
    }

    public static void setScheduleTaskId(Long scheduleTaskId, JobExecutionContext context) {
        context.getTrigger().getJobDataMap().put(SCHEDULE_TASK_ID, scheduleTaskId);
    }

    public static Long getScheduleTaskId(JobExecutionContext context) {
        return context.getTrigger().getJobDataMap().getLong(SCHEDULE_TASK_ID);
    }

    public static void setScheduleTaskParameters(ScheduleTaskParameters taskParameters, JobExecutionContext context) {
        context.getTrigger().getJobDataMap().put(SCHEDULE_TASK_PARAMETERS, taskParameters);
    }

    public static String getScheduleTaskParametersJson(JobExecutionContext context) {
        return context.getTrigger().getJobDataMap().getString(SCHEDULE_TASK_PARAMETERS);
    }

    public static DatabaseChangeParameters getDatabaseChangeTaskParameters(JobExecutionContext context) {
        return JsonUtils.fromJson(getScheduleTaskParametersJson(context), DatabaseChangeParameters.class);
    }

    public static SqlPlanParameters getSqlPlanParameters(JobExecutionContext context) {
        return JsonUtils.fromJson(getScheduleTaskParametersJson(context), SqlPlanParameters.class);
    }

    public static DataArchiveParameters getDataArchiveParameters(JobExecutionContext context) {
        return JsonUtils.fromJson(getScheduleTaskParametersJson(context), DataArchiveParameters.class);
    }

    public static DataDeleteParameters getDataDeleteParameters(JobExecutionContext context) {
        return JsonUtils.fromJson(getScheduleTaskParametersJson(context), DataDeleteParameters.class);
    }

    public static DataArchiveClearParameters getDataArchiveClearParameters(JobExecutionContext context) {
        return JsonUtils.fromJson(getScheduleTaskParametersJson(context), DataArchiveClearParameters.class);
    }


    public static DataArchiveRollbackParameters getDataArchiveRollbackParameters(JobExecutionContext context) {
        return JsonUtils.fromJson(getScheduleTaskParametersJson(context), DataArchiveRollbackParameters.class);
    }

    public static LoadDataParameters getLoadDataParameters(JobExecutionContext context) {
        return JsonUtils.fromJson(getScheduleTaskParametersJson(context), LoadDataParameters.class);
    }

    public static LogicalDatabaseChangeParameters getLogicalDatabaseChangeParameters(JobExecutionContext context) {
        return JsonUtils.fromJson(getScheduleTaskParametersJson(context), LogicalDatabaseChangeParameters.class);
    }


}
