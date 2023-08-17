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
package com.oceanbase.tools.dbbrowser.editor.util;

import java.util.Arrays;

import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBIndexType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

/**
 * @Author: Lebie
 * @Date: 2022/10/18 下午4:03
 * @Description: []
 */
public class DBObjectTestUtils {

    public static DBTableConstraint getOldUK() {
        DBTableConstraint oldUniqueKey = new DBTableConstraint();
        oldUniqueKey.setSchemaName("schema");
        oldUniqueKey.setTableName("table");
        oldUniqueKey.setName("old_unique_key");
        oldUniqueKey.setType(DBConstraintType.UNIQUE_KEY);
        return oldUniqueKey;
    }

    public static DBTableConstraint getNewUK() {
        DBTableConstraint newUniqueKey = new DBTableConstraint();
        newUniqueKey.setSchemaName("schema");
        newUniqueKey.setTableName("table");
        newUniqueKey.setName("new_unique_key");
        newUniqueKey.setType(DBConstraintType.UNIQUE_KEY);
        return newUniqueKey;
    }

    public static DBTableConstraint getOldFK() {
        DBTableConstraint oldFK = new DBTableConstraint();
        oldFK.setSchemaName("schema");
        oldFK.setTableName("table");
        oldFK.setName("old_fk");
        oldFK.setType(DBConstraintType.FOREIGN_KEY);
        oldFK.setColumnNames(Arrays.asList("c1"));
        oldFK.setReferenceColumnNames(Arrays.asList("c1", "c2"));
        oldFK.setReferenceTableName("tb");
        oldFK.setReferenceSchemaName("schema");
        return oldFK;
    }

    public static DBTableConstraint getNewFK() {
        DBTableConstraint newFK = new DBTableConstraint();
        newFK.setSchemaName("schema");
        newFK.setTableName("table");
        newFK.setName("new_fk");
        newFK.setType(DBConstraintType.FOREIGN_KEY);
        newFK.setColumnNames(Arrays.asList("c1"));
        newFK.setReferenceColumnNames(Arrays.asList("c3"));
        newFK.setReferenceTableName("tb");
        newFK.setReferenceSchemaName("schema");
        return newFK;
    }

    public static DBTableIndex getNewUniqueIndex() {
        DBTableIndex newUniqueIndex = new DBTableIndex();
        newUniqueIndex.setSchemaName("schema");
        newUniqueIndex.setTableName("table");
        newUniqueIndex.setName("new_unique_index");
        newUniqueIndex.setType(DBIndexType.UNIQUE);
        newUniqueIndex.setColumnNames(Arrays.asList("a", "b"));
        return newUniqueIndex;
    }

    public static DBTableIndex getOldUniqueIndex() {
        DBTableIndex newUniqueIndex = new DBTableIndex();
        newUniqueIndex.setSchemaName("schema");
        newUniqueIndex.setTableName("table");
        newUniqueIndex.setName("old_unique_index");
        newUniqueIndex.setType(DBIndexType.UNIQUE);
        newUniqueIndex.setColumnNames(Arrays.asList("a"));
        return newUniqueIndex;
    }
}
