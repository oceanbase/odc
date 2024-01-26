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
package com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.drop;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;

import com.oceanbase.odc.plugin.task.api.partitionplan.invoker.drop.KeepMostRecentPartitionGenerator;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionDefinition;

import lombok.NonNull;

/**
 * {@link OBMySQLKeepLatestPartitionGenerator}
 *
 * @author yh263208
 * @date 2024-01-24 15:50
 * @since ODC_release_4.2.4
 * @see KeepMostRecentPartitionGenerator
 */
public class OBMySQLKeepLatestPartitionGenerator implements KeepMostRecentPartitionGenerator {

    @Override
    public List<DBTablePartitionDefinition> generate(@NonNull Connection connection,
            @NonNull DBTable dbTable, @NonNull Integer keepCount) {
        Validate.isTrue(keepCount > 0, "Keep count can not be smaller that zero");
        AtomicLong counter = new AtomicLong(keepCount);
        List<DBTablePartitionDefinition> reverse = reverse(dbTable.getPartition().getPartitionDefinitions());
        return reverse.stream().filter(d -> counter.decrementAndGet() < 0).collect(Collectors.toList());
    }

    private List<DBTablePartitionDefinition> reverse(List<DBTablePartitionDefinition> defs) {
        List<DBTablePartitionDefinition> returnVal = new ArrayList<>();
        for (int i = defs.size() - 1; i >= 0; i--) {
            returnVal.add(defs.get(i));
        }
        return returnVal;
    }

}
