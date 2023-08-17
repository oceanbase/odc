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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection initializer, used to initialize the {@link java.sql.Connection}
 *
 * @author yh263208
 * @date 2021-11-10 15:40
 * @since ODC_release_3.2.2
 */
public interface ConnectionInitializer {
    /**
     * Initialization method, use this method to initialize business-related connections
     *
     * @param connection new {@link Connection}
     */
    void init(Connection connection) throws SQLException;

}

