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
package com.oceanbase.odc.service.db.schema.syncer;

import java.sql.Connection;

import org.springframework.core.Ordered;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/9 17:03
 */
public interface DBSchemaSyncer extends Ordered {

    /**
     * Sync database metadata
     * 
     * @param connection JDBC connection that is connected to the target database
     * @param database target database, refer to {@link Database}
     * @param dialectType dialect type, refer to {@link DialectType}
     */
    void sync(@NonNull Connection connection, @NonNull Database database, @NonNull DialectType dialectType);

    /**
     * Check if the synchronizer supports the dialect type
     * 
     * @param dialectType dialect type, refer to {@link DialectType}
     * @return true if the synchronizer supports the dialect type, otherwise false
     */
    boolean supports(@NonNull DialectType dialectType);

    /**
     * Get the object type that the synchronizer supports
     *
     * @return object type, refer to {@link DBObjectType}
     */
    DBObjectType getObjectType();

}
