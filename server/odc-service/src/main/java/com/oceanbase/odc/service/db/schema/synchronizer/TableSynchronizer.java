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
package com.oceanbase.odc.service.db.schema.synchronizer;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/9 17:18
 */
@Component
public class TableSynchronizer extends AbstractDBObjectSynchronizer {

    @Override
    protected Set<String> getLatestObjectNames(@NonNull DBSchemaAccessor accessor, @NonNull Database database) {
        return new HashSet<>(accessor.showTables(database.getName()));
    }

    @Override
    DBObjectType getObjectType() {
        return DBObjectType.TABLE;
    }

    @Override
    public boolean support(@NonNull DialectType dialectType) {
        return dialectType.isMysql() || dialectType.isOracle() || dialectType.isDoris();
    }

}
