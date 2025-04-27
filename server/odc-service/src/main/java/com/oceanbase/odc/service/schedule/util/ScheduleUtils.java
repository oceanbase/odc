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
package com.oceanbase.odc.service.schedule.util;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.schedule.model.TriggerConfig;
import com.oceanbase.odc.service.schedule.model.TriggerStrategy;

/**
 * @Author: ysj
 * @Date: 2025/4/27 15:00
 * @Since: 4.3.4
 * @Description: schedule tool class
 */
public final class ScheduleUtils {

    public static boolean isPeriodical(String triggerConfigJson) {
        TriggerConfig triggerConfig = JsonUtils.fromJson(triggerConfigJson, TriggerConfig.class);
        return triggerConfig != null && TriggerStrategy.isPeriodical(triggerConfig.getTriggerStrategy());
    }

}
