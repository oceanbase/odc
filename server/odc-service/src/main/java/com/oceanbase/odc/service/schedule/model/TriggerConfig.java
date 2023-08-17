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

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import com.oceanbase.odc.common.util.StringUtils;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2022/11/21 11:08
 * @Descripition: 自动运行任务触发器配置
 */

@Data
public class TriggerConfig implements Serializable {
    private static final long serialVersionUID = 7593442666060425730L;
    private TriggerStrategy triggerStrategy;
    private Set<Integer> days;
    private Set<Integer> hours;
    private String cronExpression;
    private Date startAt;

    public String getCronExpression() {
        switch (this.getTriggerStrategy()) {
            case WEEK:
                return String.format("0 0 %s ? * %s", StringUtils.join(getHours(), ','),
                        StringUtils.join(getDays(), ','));
            case MONTH:
                return String.format("0 0 %s %s * ?", StringUtils.join(getHours(), ','),
                        StringUtils.join(getDays(), ','));
            case DAY:
                return String.format("0 0 %s * * ?", StringUtils.join(getHours(), ','));
            case CRON:
                return cronExpression;
            default:
                return null;
        }
    }

    public boolean isSingleTrigger() {
        return triggerStrategy == TriggerStrategy.START_AT || triggerStrategy == TriggerStrategy.START_NOW;
    }
}
