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

import javax.validation.constraints.Size;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2024/8/30 16:41
 * @Description: []
 */
@Data
public class LogicalDatabaseChangeParameters implements ScheduleTaskParameters {
    @Size(max = 16777215, message = "The sql size should be less than 16,777,215 bytes")
    private String sqlContent;
    private String delimiter;
    private Long timeoutMillis;
    private Long databaseId;
}
