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

import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;

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

}
