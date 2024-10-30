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
package com.oceanbase.odc.service.db.schema;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * {@link GlobalSearchProperties}
 *
 * @author yh263208
 * @date 2024-07-08 14:51
 * @since ODC_release_4.3.1
 */
@Data
@RefreshScope
@Configuration
public class GlobalSearchProperties {

    @Value("${odc.database.schema.global-search.enabled:true}")
    private boolean enableGlobalSearch;
    @Value("${odc.database.schema.global-search.max-pending-hours:1}")
    private long maxPendingHours;

    public long getMaxPendingHours() {
        return this.maxPendingHours <= 0 ? 1 : this.maxPendingHours;
    }

    public long getMaxPendingMillis() {
        return TimeUnit.MILLISECONDS.convert(getMaxPendingHours(), TimeUnit.HOURS);
    }

}
