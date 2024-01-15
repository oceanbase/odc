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
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import com.oceanbase.tools.dbbrowser.util.StringUtils;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/1/12
 * @since ODC_release_4.2.4
 */
public class OBMySQLLessThan400TableEditor extends OBMySQLTableEditor {

    public OBMySQLLessThan400TableEditor(DBObjectEditor<DBTableIndex> indexEditor,
            DBObjectEditor<DBTableColumn> columnEditor,
            DBObjectEditor<DBTableConstraint> constraintEditor, DBObjectEditor<DBTablePartition> partitionEditor) {
        super(indexEditor, columnEditor, constraintEditor, partitionEditor);
    }

    @Override
    public void generateUpdateTableOptionDDL(@NonNull DBTable oldTable, @NonNull DBTable newTable,
            @NonNull SqlBuilder sqlBuilder) {
        if (Objects.isNull(oldTable.getTableOptions()) || Objects.isNull(newTable.getTableOptions())) {
            return;
        }
        generateUpdateTableCommentDDL(oldTable, newTable, sqlBuilder);
        if (!StringUtils.equals(oldTable.getTableOptions().getCharsetName(),
                newTable.getTableOptions().getCharsetName())) {
            sqlBuilder.append("/* Unsupported operation to modify table charset */\n");
        }
        if (!StringUtils.equals(oldTable.getTableOptions().getCollationName(),
                newTable.getTableOptions().getCollationName())) {
            sqlBuilder.append("/* Unsupported operation to modify table collation */\n");
        }
    }
}
