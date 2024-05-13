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
package com.oceanbase.tools.dbbrowser.editor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import com.oceanbase.tools.dbbrowser.model.DBColumnGroupElement;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBForeignKeyMatchType;
import com.oceanbase.tools.dbbrowser.model.DBForeignKeyModifyRule;
import com.oceanbase.tools.dbbrowser.model.DBIndexAlgorithm;
import com.oceanbase.tools.dbbrowser.model.DBTable;
import com.oceanbase.tools.dbbrowser.model.DBTable.DBTableOptions;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

import lombok.SneakyThrows;

/**
 * @Author: Lebie
 * @Date: 2022/8/1 下午8:09
 * @Description: []
 */
public class DBObjectUtilsTest {

    public static DBTableColumn getNewColumn() {
        DBTableColumn column = new DBTableColumn();
        column.setTableName("whatever_table");
        column.setSchemaName("whatever_schema");
        column.setName("COL1");
        column.setTypeName("NUMBER");
        column.setPrecision(2L);
        column.setScale(1);
        column.setOrdinalPosition(2);
        column.setDefaultValue("1");
        column.setNullable(false);
        return column;
    }

    public static DBTableColumn getOldColumn() {
        DBTableColumn column = getNewColumn();
        column.setName("OLD_COL1");
        column.setOrdinalPosition(1);
        return column;
    }

    public static DBTable getNewTable() {
        DBTable table = new DBTable();
        table.setSchemaName("whatever_schema");
        table.setName("whatever_table");
        table.setColumns(Arrays.asList(getNewColumn(), getOldColumn()));
        table.setConstraints(Arrays.asList(getOldFKConstraint(), getOldUKConstraint()));
        table.setIndexes(Arrays.asList(getNewIndex(), getOldIndex()));
        DBColumnGroupElement cg = new DBColumnGroupElement();
        cg.setEachColumn(true);
        table.setColumnGroups(Collections.singletonList(cg));
        DBTableOptions options = new DBTableOptions();
        table.setTableOptions(options);
        options.setAutoIncrementInitialValue(1L);
        options.setCharsetName("utf8mb4");
        options.setCollationName("utf8mb4_bin");
        options.setRowFormat("DYNAMIC");
        options.setCompressionOption("'zstd_1.0'");
        options.setReplicaNum(1);
        options.setBlockSize(1);
        options.setTabletSize(134217728L);
        options.setUseBloomFilter(false);
        options.setComment("this is a comment");
        return table;
    }

    public static DBTable getOldTable() {
        DBTable table = getNewTable();
        table.setName("old_table");
        table.getTableOptions().setCharsetName("ascii");
        table.getTableOptions().setCollationName("ascii_bin");
        table.getTableOptions().setRowFormat("COMPACT");
        return table;
    }

    public static DBTableIndex getOldIndex() {
        DBTableIndex index = new DBTableIndex();
        index.setTableName("whatever_table");
        index.setSchemaName("whatever_schema");
        index.setName("old_index");
        index.setColumnNames(Arrays.asList("col1", "col2"));
        index.setAlgorithm(DBIndexAlgorithm.BTREE);
        index.setGlobal(true);
        index.setComment("whatever_comment");
        index.setVisible(true);
        index.setCollation("A");
        index.setOrdinalPosition(1);
        return index;
    }

    public static DBTableIndex getNewIndex() {
        DBTableIndex index = getOldIndex();
        index.setName("new_index");
        index.setAlgorithm(DBIndexAlgorithm.HASH);
        index.setOrdinalPosition(2);
        return index;
    }

    public static DBTableConstraint getOldFKConstraint() {
        DBTableConstraint constraint = new DBTableConstraint();
        constraint.setTableName("old_table");
        constraint.setSchemaName("old_schema");
        constraint.setName("old_constraint");
        constraint.setOrdinalPosition(2);
        constraint.setColumnNames(Arrays.asList("col1", "col2"));
        constraint.setType(DBConstraintType.FOREIGN_KEY);
        constraint.setOnDeleteRule(DBForeignKeyModifyRule.SET_NULL);
        constraint.setOnUpdateRule(DBForeignKeyModifyRule.NO_ACTION);
        constraint.setReferenceSchemaName("ref_schema");
        constraint.setReferenceTableName("ref_table");
        constraint.setReferenceColumnNames(Arrays.asList("ref_col1", "ref_col2"));
        constraint.setMatchType(DBForeignKeyMatchType.FULL);
        return constraint;
    }

    public static DBTableConstraint getNewFKConstraint() {
        DBTableConstraint constraint = getOldFKConstraint();
        constraint.setName("new_constraint");
        return constraint;
    }

    public static DBTableConstraint getOldUKConstraint() {
        DBTableConstraint constraint = new DBTableConstraint();
        constraint.setTableName("old_table");
        constraint.setSchemaName("old_schema");
        constraint.setName("new_constraint");
        constraint.setOrdinalPosition(1);
        constraint.setColumnNames(Arrays.asList("col1", "col2"));
        constraint.setType(DBConstraintType.UNIQUE_KEY);
        return constraint;
    }

    public static DBTableConstraint getNewUKConstraint() {
        DBTableConstraint constraint = getOldUKConstraint();
        constraint.setName("new_fk");
        return constraint;
    }

    @SneakyThrows
    public static String loadAsString(String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String path : paths) {
            sb.append(readFile(path));
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String readFile(String strFile) throws IOException {
        try (InputStream input = new FileInputStream(strFile)) {
            int available = input.available();
            byte[] bytes = new byte[available];
            input.read(bytes);
            return new String(bytes);
        }
    }

}
