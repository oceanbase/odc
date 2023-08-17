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
package com.oceanbase.odc.service.schedule.flowtask;

import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.processor.FlowTaskPreprocessor;
import com.oceanbase.odc.service.flow.processor.Preprocessor;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;

/**
 * @Author：tinker
 * @Date: 2023/7/26 19:49
 * @Descripition:
 */
@FlowTaskPreprocessor(type = TaskType.PARTITION_PLAN)
public class PartitionPlanPreprocessor implements Preprocessor {

    @Autowired
    private PartitionPlanService partitionPlanService;

    @Override
    public void process(CreateFlowInstanceReq req) {
        // TODO
    }
}
