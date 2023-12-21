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

import java.io.Serializable;

import com.oceanbase.odc.core.flow.model.TaskParameters;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * config information for data mock task
 *
 * @author yh263208
 * @date 2021-01-20 15:46
 * @since ODC_release_2.4.0
 */
@Setter
@Getter
@EqualsAndHashCode
public class OdcMockTaskConfig implements Serializable, TaskParameters {
    /**
     * task id for data mock task
     */
    private String id;
    /**
     * task type
     */
    private final CommonTaskTypeEnum taskType = CommonTaskTypeEnum.MOCK_DATA;
    /**
     * task name
     */
    private String taskName;
    /**
     * task detail, json string
     */
    private String taskDetail;
}
