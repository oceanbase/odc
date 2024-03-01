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
package com.oceanbase.odc.service.flow.task.model;

import com.oceanbase.odc.core.flow.model.FlowTaskResult;

import lombok.Getter;
import lombok.Setter;

/**
 * @author tianke
 * @author yh263208
 * @date 2024-02-28 11:25
 * @since ODC_release_4.2.4
 */
@Getter
@Setter
public class PartitionPlanTaskResult implements FlowTaskResult {
    private boolean success;
}
