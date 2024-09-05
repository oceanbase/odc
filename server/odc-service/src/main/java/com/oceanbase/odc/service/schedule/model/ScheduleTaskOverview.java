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
package com.oceanbase.odc.service.schedule.model;

import java.util.Date;

import com.oceanbase.odc.core.shared.constant.TaskStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author：tinker
 * @Date: 2024/6/19 14:31
 * @Descripition:
 */


@Data
@Accessors(chain = true)
public class ScheduleTaskOverview {

    private Long id;

    private ScheduleTaskType type;

    private TaskStatus status;

    private Date createTime;

    private Date updateTime;

    /**
     * Only used in version 4.3.2, it will be deleted after version 4.3.3.
     */
    private String jobGroup;

    private String fullLogDownloadUrl;

    public String getJobGroup() {
        return type == null ? null : type.name();
    }
}
