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

import org.springframework.core.Ordered;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/9 17:03
 */
public interface DBSchemaSyncer extends Ordered {

    /**
     * Sync database metadata
     * 
     * @param accessor db schema accessor, refer to {@link DBSchemaAccessor}
     * @param database target database, refer to {@link Database}
     */
    void sync(@NonNull DBSchemaAccessor accessor, @NonNull Database database);

    /**
     * Get the object type that the synchronizer supports
     *
     * @return object type, refer to {@link DBObjectType}
     */
    DBObjectType getObjectType();

    /**
     * Whether the synchronizer supports the specified dialect type
     * 
     * @param dialectType dialect type
     * @return true if support
     */
    boolean supports(@NonNull DialectType dialectType);

}
