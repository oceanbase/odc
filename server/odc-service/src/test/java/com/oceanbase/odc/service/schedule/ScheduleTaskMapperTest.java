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
package com.oceanbase.odc.service.schedule;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.metadb.schedule.ScheduleTaskEntity;
import com.oceanbase.odc.service.loaddata.model.LoadDataParameters;
import com.oceanbase.odc.service.schedule.model.ScheduleTask;
import com.oceanbase.odc.service.schedule.model.ScheduleTaskMapper;

/**
 * @author: liuyizhuo.lyz
 * @date: 2024/9/20
 */
public class ScheduleTaskMapperTest {

    private final ScheduleTaskMapper mapper = ScheduleTaskMapper.INSTANCE;

    @Test
    public void modelToEntity_withParameters() {
        ScheduleTask task = new ScheduleTask();
        task.setParameters(new LoadDataParameters());

        ScheduleTaskEntity entity = mapper.modelToEntity(task);
        Assert.assertNotNull(entity.getParametersJson());
    }

}
