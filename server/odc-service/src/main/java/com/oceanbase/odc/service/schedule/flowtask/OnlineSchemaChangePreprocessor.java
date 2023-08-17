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
import com.oceanbase.odc.service.onlineschemachange.validator.OnlineSchemaChangeValidator;

@FlowTaskPreprocessor(type = TaskType.ONLINE_SCHEMA_CHANGE)
public class OnlineSchemaChangePreprocessor implements Preprocessor {
    @Autowired
    private OnlineSchemaChangeValidator validator;

    @Override
    public void process(CreateFlowInstanceReq req) {
        validator.validate(req);
    }
}
