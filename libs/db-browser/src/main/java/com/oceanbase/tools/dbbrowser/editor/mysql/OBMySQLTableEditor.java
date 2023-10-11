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

import java.util.Objects;

import com.oceanbase.tools.dbbrowser.editor.DBObjectEditor;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;

/**
 * @author jingtian
 * @date 2023/9/25
 * @since ODC_release_4.2.2
 */
public class OBMySQLTableEditor extends MySQLTableEditor {
    public OBMySQLTableEditor(DBObjectEditor<DBTableIndex> indexEditor,
            DBObjectEditor<DBTableColumn> columnEditor,
            DBObjectEditor<DBTableConstraint> constraintEditor,
            DBObjectEditor<DBTablePartition> partitionEditor) {
        super(indexEditor, columnEditor, constraintEditor, partitionEditor);
    }

    @Override
    protected void appendMoreTableOptions(DBTable table, SqlBuilder sqlBuilder) {
        if (Objects.isNull(table.getTableOptions())) {
            return;
        }

        DBTableOptions options = table.getTableOptions();
        if (Objects.nonNull(options.getReplicaNum())) {
            sqlBuilder.append("REPLICA_NUM = ").append(String.valueOf(options.getReplicaNum())).space();
        }
        if (Objects.nonNull(options.getUseBloomFilter())) {
            sqlBuilder.append("USE_BLOOM_FILTER = ").append(options.getUseBloomFilter() ? "TRUE" : "FALSE").space();
        }
        if (Objects.nonNull(options.getTabletSize())) {
            sqlBuilder.append("TABLET_SIZE = ").append(String.valueOf(options.getTabletSize())).space();
        }
    }
}
