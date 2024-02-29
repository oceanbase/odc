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

package com.oceanbase.odc.service.task.schedule;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Identity a unique job
 * 
 * @author yaobin
 * @date 2023-11-23
 * @since 4.2.4
 */
@EqualsAndHashCode
@Data
public class JobIdentity {

    /**
     * job id
     */
    private Long id;

    public static JobIdentity of(Long id) {
        JobIdentity jobIdentity = new JobIdentity();
        jobIdentity.setId(id);
        return jobIdentity;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
