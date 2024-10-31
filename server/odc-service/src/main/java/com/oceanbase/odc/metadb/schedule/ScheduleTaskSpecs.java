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
package com.oceanbase.odc.metadb.schedule;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.core.shared.constant.TaskStatus;

/**
 * @Author：tinker
 * @Date: 2023/5/24 16:46
 * @Descripition:
 */
public class ScheduleTaskSpecs {

    public static Specification<ScheduleTaskEntity> jobNameEquals(String jobName) {
        return SpecificationUtil.columnEqual("jobName", jobName);
    }

    public static Specification<ScheduleTaskEntity> jobGroupEquals(String jobGroup) {
        return SpecificationUtil.columnEqual("jobGroup", jobGroup);
    }

    public static Specification<ScheduleTaskEntity> jobNameIn(Set<String> jobName) {
        return SpecificationUtil.columnIn("jobName", jobName);
    }

    public static Specification<ScheduleTaskEntity> idEquals(Long id) {
        return SpecificationUtil.columnEqual("id", id);
    }

    public static Specification<ScheduleTaskEntity> statusIn(List<TaskStatus> jobStatuses) {
        return SpecificationUtil.columnIn("status", jobStatuses);
    }

    public static Specification<ScheduleTaskEntity> fireTimeBefore(Date fireTime) {
        return SpecificationUtil.columnBefore("fireTime", fireTime);
    }

    public static Specification<ScheduleTaskEntity> fireTimeLate(Date fireTime) {
        return SpecificationUtil.columnLate("fireTime", fireTime);
    }

}
