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
package com.oceanbase.odc.service.schedule.job;

import java.util.List;

import com.oceanbase.odc.core.shared.constant.TaskErrorStrategy;

import lombok.Data;

@Data
public class PublishSqlPlanJobReq {

    private String sqlContent;

    private List<String> sqlObjectIds;

    private String delimiter;

    private Integer retryTimes;

    private Long retryIntervalMillis = 10000L;

    private Integer queryLimit;

    private Long timeoutMillis;

    private TaskErrorStrategy errorStrategy;

    private String sessionTimeZone;

    private boolean needDataMasking;

}
