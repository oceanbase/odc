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
package com.oceanbase.tools.dbbrowser.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/10 12:36
 * @since: 4.3.4
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DBMaterializedViewRefreshSchedule {

    private StartStrategy startStrategy;

    private Date startWith;

    private Long interval;

    private TimeUnit unit;

    // These properties are used to demonstrate the automatic refresh configuration
    private String startExpression;

    private String nextExpression;

    public enum TimeUnit {
        SECOND,
        MINUTE,
        HOUR,
        DAY,
        WEEK,
        MONTH,
        YEAR;
    }

    public enum StartStrategy {
        START_NOW,
        START_AT;
    }

}
