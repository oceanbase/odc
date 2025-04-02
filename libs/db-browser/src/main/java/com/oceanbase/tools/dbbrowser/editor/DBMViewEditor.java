/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.tools.dbbrowser.editor;


import com.oceanbase.tools.dbbrowser.model.DBMaterializedView;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.util.SqlBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/4/1 21:45
 * @since: 4.3.4
 */
public abstract class DBMViewEditor implements DBObjectEditor<DBMaterializedView>{
    @Getter
    @Setter
    protected DBObjectEditor<DBTableIndex> indexEditor;

    public DBMViewEditor(DBObjectEditor<DBTableIndex> indexEditor) {
        this.indexEditor = indexEditor;
    }


    @Override
    public String generateUpdateObjectDDL(@NotNull DBMaterializedView oldMView,
                                          @NotNull DBMaterializedView newMView) {
        SqlBuilder sqlBuilder = sqlBuilder();
        if (!StringUtils.equals(oldMView.getName(), newMView.getName())) {
            sqlBuilder.append(generateRenameObjectDDL(oldMView, newMView));
            sqlBuilder.append(";\n");
        }
        sqlBuilder.append(indexEditor.generateUpdateObjectListDDL(oldMView.getIndexes(), newMView.getIndexes()));
        return sqlBuilder.toString();
    }

    @Override
    public String generateUpdateObjectListDDL(Collection<DBTable> oldTables,
                                              Collection<DBTable> newTables) {
        throw new NotImplementedException();
    }

}
