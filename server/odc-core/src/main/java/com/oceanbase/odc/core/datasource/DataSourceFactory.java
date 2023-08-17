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
package com.oceanbase.odc.core.datasource;

import javax.sql.DataSource;

/**
 * Data source factory class, used to generate {@link javax.sql.DataSource} The factory class
 * initializes the data source object according to the initial configuration passed in by the user.
 * Use the {@code #getDataSource()} method to obtain the {@link javax.sql.DataSource}
 *
 * @author yh263208
 * @date 2021-11-10 14:48
 * @since ODC_release_3.2.2
 */
public interface DataSourceFactory {
    /**
     * Get data source method, used to get a {@link DataSource}
     *
     * @return {@link DataSource}
     */
    DataSource getDataSource();

}
