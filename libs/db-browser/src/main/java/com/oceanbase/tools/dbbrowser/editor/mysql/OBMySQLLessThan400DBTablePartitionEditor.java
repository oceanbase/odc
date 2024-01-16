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
package com.oceanbase.tools.dbbrowser.editor.mysql;

import javax.validation.constraints.NotNull;

import com.oceanbase.tools.dbbrowser.model.DBTablePartition;

/**
 * @author jingtian
 * @date 2024/1/12
 * @since ODC_release_4.2.4
 */
public class OBMySQLLessThan400DBTablePartitionEditor extends MySQLDBTablePartitionEditor {
    @Override
    public String generateCreateObjectDDL(DBTablePartition partition) {
        return "/* Unsupported operation to convert non-partitioned table to partitioned table */\n";
    }

    @Override
    protected String modifyPartitionType(@NotNull DBTablePartition oldPartition,
            @NotNull DBTablePartition newPartition) {
        return "/* Unsupported operation to modify table partition type */\n";
    }
}
