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
package com.oceanbase.odc.quartz;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.quartz.ScheduleTaskJobServiceImpl;
import com.oceanbase.odc.service.schedule.model.CreateQuartzJobParam;
import com.oceanbase.odc.service.schedule.model.QuartzKeyGenerator;
import com.oceanbase.odc.service.schedule.model.ScheduleType;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;

/**
 * @Author：tinker
 * @Date: 2023/5/19 10:23
 * @Descripition:
 */
public class ScheduleTaskJobServiceImplTest extends ServiceTestEnv {

    @Autowired
    @Qualifier("scheduleTaskJobServiceImpl")
    private ScheduleTaskJobServiceImpl quartzJobService;

    @Test
    public void create() throws SchedulerException, ParseException {
        CreateQuartzJobParam req = new CreateQuartzJobParam();
        req.setJobKey(QuartzKeyGenerator.generateJobKey("1", ScheduleType.SQL_PLAN.name()));
        TriggerConfig config = new TriggerConfig();
        config.setTriggerStrategy(TriggerStrategy.START_AT);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd hh:mm:ss");
        Date date = simpleDateFormat.parse("20220101 00:00:00");
        config.setStartAt(date);
        req.setTriggerConfig(config);
        quartzJobService.createJob(req);
        Trigger trigger =
                quartzJobService.getTrigger(QuartzKeyGenerator.generateTriggerKey("1", ScheduleType.SQL_PLAN.name()));
        Assert.assertEquals(trigger.getFinalFireTime(), date);
    }

}
