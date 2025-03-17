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
package com.oceanbase.odc.service.flow.model;

import java.util.Date;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.TaskType;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author: ysj
 * @Date: 2025/2/25 16:45
 * @Since: 4.3.4
 * @Description:
 */
@Data
@Accessors(chain = true)
@Builder
public class InnerQueryFlowInstanceParams {

    private Set<Long> flowInstanceIds;
    private Set<Long> parentInstanceIds;
    private Set<TaskType> taskTypes;
    private Date startTime;
    private Date endTime;

}
