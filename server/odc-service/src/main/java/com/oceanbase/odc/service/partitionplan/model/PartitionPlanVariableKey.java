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
package com.oceanbase.odc.service.partitionplan.model;

import com.oceanbase.odc.common.i18n.Translatable;

/**
 * {@link PartitionPlanVariableKey}
 *
 * @author yh263208
 * @date 2024-01-09 16:03
 * @since ODC_release_4.2.4
 * @see com.oceanbase.odc.common.i18n.Translatable
 */
public enum PartitionPlanVariableKey implements Translatable {
    /**
     * the value it depends on user's input
     */
    INTERVAL,
    /**
     * value of last partition, eg. <code>
     *     CREATE TABLE `range_parti_tbl` (
     *      `id` varchar(64) DEFAULT NULL,
     *      `birthday` date NOT NULL
     *     ) partition by range columns(birthday)(
     *      partition p0 values less than ('2020-12-31'),
     *      partition p1 values less than ('2021-12-31'))
     * </code> the last partition's value is {@code '2021-12-31'}
     */
    LAST_PARTITION_VALUE;

    @Override
    public String code() {
        return name();
    }

    public String getVariable() {
        return "${" + name() + "}";
    }

}
