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
package com.oceanbase.odc.plugin.task.api.partitionplan.model;

import com.oceanbase.odc.plugin.task.api.partitionplan.datatype.TimeDataType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link TimeIncreaseGeneratorConfig}
 *
 * @author yh263208
 * @date 2024-01-19 17:23
 * @since ODC_release_4.2.4
 */
@Getter
@Setter
@ToString
public class TimeIncreaseGeneratorConfig {

    private long baseTimestampMillis;
    private int interval;
    /**
     * ref {@link TimeDataType#getPrecision()}
     */
    private int intervalPrecision;
    private boolean fromCurrentTime;

    private String timeFormat;

}
