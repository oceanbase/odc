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
package com.oceanbase.odc.service.schedule.job;

import static com.oceanbase.odc.core.shared.constant.OdcConstants.CREATOR_ID;
import static com.oceanbase.odc.core.shared.constant.OdcConstants.FLOW_TASK_ID;
import static com.oceanbase.odc.core.shared.constant.OdcConstants.SCHEDULE_ID;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.onlineschemachange.OnlineSchemaChangeContextHolder;
import com.oceanbase.odc.service.onlineschemachange.OnlineSchemaChangeTaskHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yaobin
 * @date 2023-06-08
 * @since 4.2.0
 */
@Slf4j
public class OnlineSchemaChangeCompleteJob implements OdcJob {
    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        OnlineSchemaChangeContextHolder.trace((String) (jobDataMap.get(CREATOR_ID)),
                (String) jobDataMap.get(FLOW_TASK_ID), (String) jobDataMap.get(OdcConstants.ORGANIZATION_ID));
        try {
            log.info("Start execute {}", getClass().getSimpleName());
            Long scheduleId = (Long) context.getMergedJobDataMap().get(SCHEDULE_ID);

            OnlineSchemaChangeTaskHandler onlineSchemaChangeTaskHandler =
                    SpringContextUtil.getBean(OnlineSchemaChangeTaskHandler.class);
            onlineSchemaChangeTaskHandler.complete(scheduleId);
        } finally {
            OnlineSchemaChangeContextHolder.clear();
        }
    }

    @Override
    public void interrupt() {
        throw new UnsupportedException();
    }

}
