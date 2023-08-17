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
import lombok.NonNull;

/**
 * @Author：tinker
 * @Date: 2023/5/24 15:35
 * @Descripition:
 */

@Data
public class ScheduleTaskResp {

    private Long id;

    private String jobName;

    private String jobGroup;

    private TaskStatus status;

    private double progressPercentage;

    private String resultJson;

    private Date createTime;

    private Date updateTime;

    public static ScheduleTaskResp withId(@NonNull Long id) {
        ScheduleTaskResp resp = new ScheduleTaskResp();
        resp.setId(id);
        return resp;
    }

}
