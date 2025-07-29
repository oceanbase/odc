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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

/**
 * used for schedule approve filter
 * 
 * @author longpeng.zlp
 * @date 2025/7/15 15:40
 *
 */
public enum ScheduleApproveFilterStatus {
    APPROVING(Arrays.asList(ScheduleChangeStatus.APPROVING)),
    APPROVE_FAILED(Arrays.asList(ScheduleChangeStatus.APPROVE_EXPIRED, ScheduleChangeStatus.APPROVE_CANCELED,
            ScheduleChangeStatus.APPROVE_REJECTED));

    private final List<ScheduleChangeStatus> statuses;

    public List<ScheduleChangeStatus> getStatuses() {
        return statuses;
    }

    ScheduleApproveFilterStatus(List<ScheduleChangeStatus> statuses) {
        this.statuses = statuses;
    }

    public static Set<ScheduleChangeStatus> getApproveFilterStatuses(Collection<ScheduleApproveFilterStatus> statuses) {
        if (CollectionUtils.isEmpty(statuses)) {
            return Collections.emptySet();
        }
        Set<ScheduleChangeStatus> ret = new HashSet<>();
        for (ScheduleApproveFilterStatus status : statuses) {
            ret.addAll(status.getStatuses());
        }
        return ret;
    }
}
