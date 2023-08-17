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

package com.oceanbase.odc.service.onlineschemachange.pipeline;

import java.util.function.Supplier;

/**
 * @author yaobin
 * @date 2023-08-09
 * @since 4.2.0
 */
public interface DataSourceCreator {

    /**
     * try to create datasource and return datasource id when condition is receive before timeout
     *
     * @param createDataSource to create datasource and return datasource id
     * @param condition precondition
     * @param timeout wait precondition timeout
     * @return
     */
    String create(Supplier<String> createDataSource, Supplier<Boolean> condition, int timeout);
}
