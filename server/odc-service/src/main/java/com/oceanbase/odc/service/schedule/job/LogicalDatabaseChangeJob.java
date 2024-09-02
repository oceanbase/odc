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

package com.oceanbase.odc.service.schedule.job;

import org.quartz.JobExecutionContext;

/**
 * @Author: Lebie
 * @Date: 2024/8/30 16:08
 * @Description: []
 */
public class LogicalDatabaseChangeJob implements OdcJob{
    @Override
    public void execute(JobExecutionContext context) {

    }

    @Override
    public void before(JobExecutionContext context) {

    }

    @Override
    public void after(JobExecutionContext context) {

    }

    @Override
    public void interrupt() {

    }
}
