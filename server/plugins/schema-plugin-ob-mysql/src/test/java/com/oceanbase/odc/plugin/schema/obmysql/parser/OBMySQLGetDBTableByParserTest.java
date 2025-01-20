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
package com.oceanbase.odc.plugin.schema.obmysql.parser;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBForeignKeyModifyRule;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

/**
 * @author jingtian
 * @date 2023/7/6
 */
public class OBMySQLGetDBTableByParserTest {

    @Test
    public void getPartition_Hash_use_column_1_Success() {
        String ddl = "CREATE TABLE hash_column1(id INT) PARTITION BY HASH(id) PARTITIONS 6;";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(6L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getPartitionOption().getType());
        Assert.assertEquals("id", partition.getPartitionOption().getColumnNames().get(0));
    }

    @Test
    public void getPartition_Hash_use_column_2_Success() {
        String ddl = "CREATE TABLE hash_column2(id INT) PARTITION BY HASH(id) (PARTITION t1, PARTITION t2);";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(2L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getPartitionOption().getType());
        Assert.assertEquals("id", partition.getPartitionOption().getColumnNames().get(0));
    }

    @Test
    public void getPartition_Hash_use_expression_1_Success() {
        String ddl = "CREATE TABLE hash_expression(a INT, b INT) PARTITION BY HASH(a+b) PARTITIONS 3;";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(3L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getPartitionOption().getType());
        Assert.assertEquals("a+b", partition.getPartitionOption().getExpression());
    }

    @Test
    public void getPartition_key_Success() {
        String ddl =
                "CREATE TABLE key_part(id INT,gmt_create DATETIME,info VARCHAR(20)) PARTITION BY KEY(id,gmt_create) PARTITIONS 3;";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(3L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.KEY, partition.getPartitionOption().getType());
        Assert.assertEquals(2, partition.getPartitionOption().getColumnNames().size());
    }

    @Test
    public void getPartition_range_Success() {
        String ddl = "CREATE TABLE range_part (\n"
                + "    id INT NOT NULL\n"
                + ")\n"
                + "PARTITION BY RANGE(id) (\n"
                + "    PARTITION p0 VALUES LESS THAN (6),\n"
                + "    PARTITION p1 VALUES LESS THAN (11),\n"
                + "    PARTITION p2 VALUES LESS THAN (16),\n"
                + "    PARTITION p3 VALUES LESS THAN (21)\n"
                + ");";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(4L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE, partition.getPartitionOption().getType());
        Assert.assertEquals("id", partition.getPartitionOption().getColumnNames().get(0));
    }

    @Test
    public void getPartition_range_use_expression_Success() {
        String ddl = "CREATE TABLE range_expression (\n"
                + "  id BIGINT NOT NULL,\n"
                + "  log_value VARCHAR(50),\n"
                + "  log_date TIMESTAMP NOT NULL)\n"
                + "PARTITION BY RANGE(UNIX_TIMESTAMP(log_date))\n"
                + "(PARTITION M202001 VALUES LESS THAN(UNIX_TIMESTAMP('2020/02/01')),\n"
                + " PARTITION M202002 VALUES LESS THAN(UNIX_TIMESTAMP('2020/03/01')),\n"
                + " PARTITION M202003 VALUES LESS THAN(UNIX_TIMESTAMP('2020/04/01')),\n"
                + " PARTITION M202004 VALUES LESS THAN(MAXVALUE)\n"
                + ");";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(4L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE, partition.getPartitionOption().getType());
        Assert.assertEquals("UNIX_TIMESTAMP(log_date)", partition.getPartitionOption().getExpression());
    }

    @Test
    public void getPartition_range_columns_Success() {
        String ddl = "CREATE TABLE range_columns (\n"
                + "     a INT,\n"
                + "     b INT,\n"
                + "     c CHAR(3)\n"
                + ")\n"
                + "PARTITION BY RANGE COLUMNS(a,b,c) (\n"
                + "PARTITION p0 VALUES LESS THAN (5,10,'ggg'),\n"
                + "PARTITION p1 VALUES LESS THAN (10,20,'mmm'),\n"
                + "PARTITION p2 VALUES LESS THAN (15,30,'sss'),\n"
                + "PARTITION p3 VALUES LESS THAN (MAXVALUE,MAXVALUE,MAXVALUE)\n"
                + " );";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(4L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE_COLUMNS, partition.getPartitionOption().getType());
        Assert.assertEquals(3, partition.getPartitionOption().getColumnNames().size());
    }

    @Test
    public void getPartition_list_use_expression_Success() {
        String ddl = "CREATE TABLE list_expression (col1 INT, col2 INT)\n"
                + "PARTITION BY LIST(col1+col2)\n"
                + "(PARTITION p0 VALUES IN (1, 2, 3),\n"
                + "PARTITION p1 VALUES IN (5, 6, 7),\n"
                + "PARTITION p2 VALUES IN (DEFAULT)\n"
                + ");";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(3L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST, partition.getPartitionOption().getType());
        Assert.assertEquals("col1+col2", partition.getPartitionOption().getExpression());
    }

    @Test
    public void getPartition_list_Success() {
        String ddl = "CREATE TABLE list (col INT)\n"
                + "PARTITION BY LIST(col)\n"
                + "(PARTITION p0 VALUES IN (1, 2, 3),\n"
                + "PARTITION p1 VALUES IN (5, 6, 7),\n"
                + "PARTITION p2 VALUES IN (8, 9, 10)\n"
                + ");";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(3L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST, partition.getPartitionOption().getType());
        Assert.assertEquals("col", partition.getPartitionOption().getColumnNames().get(0));
    }

    @Test
    public void getPartition_list_columns_Success() {
        String ddl = "CREATE TABLE list_columns (\n"
                + "    city VARCHAR(15),\n"
                + "    renewal DATE\n"
                + ")\n"
                + "PARTITION BY LIST COLUMNS(city,renewal) (\n"
                + "    PARTITION pRegion_1 VALUES IN(('Oskarshamn','2010-02-01'), ('Högsby','2010-02-01'), "
                + "('Mönsterås','2010-02-01')),\n"
                + "    PARTITION pRegion_2 VALUES IN(('Vimmerby','2010-03-01'), ('Hultsfred','2010-03-01'), "
                + "('Västervik','2010-03-01')),\n"
                + "    PARTITION pRegion_3 VALUES IN(('Nässjö','2010-04-01'), ('Eksjö','2010-04-01'), "
                + "('Vetlanda','2010-04-01')),\n"
                + "    PARTITION pRegion_4 VALUES IN(('Uppvidinge','2010-05-01'), ('Alvesta','2010-05-01'), "
                + "('Växjo','2010-05-01'))\n"
                + ");";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(4L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST_COLUMNS, partition.getPartitionOption().getType());
        Assert.assertEquals(2, partition.getPartitionOption().getColumnNames().size());
    }

    @Test
    public void getPartition_secondary_key_use_template_Success() {
        String ddl = "CREATE TABLE `range_2_key_template` (\n"
                + "  `col1` int(11) DEFAULT NULL,\n"
                + "  `col2` int(11) DEFAULT NULL\n"
                + ")\n"
                + "partition by range columns(`col1`) subpartition by key(col2) subpartition template (\n"
                + "subpartition p0,\n"
                + "subpartition p1,\n"
                + "subpartition p2)\n"
                + "(partition p0 values less than (100),\n"
                + "partition p1 values less than (200),\n"
                + "partition p2 values less than (300));";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(3L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE_COLUMNS, partition.getPartitionOption().getType());
        Assert.assertEquals(true, partition.getSubpartitionTemplated());
        Assert.assertEquals("col1", partition.getPartitionOption().getColumnNames().get(0));
        Assert.assertEquals(DBTablePartitionType.KEY, partition.getSubpartition().getPartitionOption().getType());
        Assert.assertEquals(1, partition.getSubpartition().getPartitionOption().getColumnNames().size());
        Assert.assertTrue(partition.getSubpartition().getPartitionOption().getPartitionsNum() == 3);
    }

    @Test
    public void getPartition_secondary_key_no_template_Success() {
        String ddl = "CREATE TABLE list_2_key_no_template (col1 INT NOT NULL,col2 varchar(50),col3 INT NOT NULL)\n"
                + "PARTITION BY LIST(col1)\n"
                + "SUBPARTITION BY KEY(`col2`, `col3`)\n"
                + "(PARTITION p0 VALUES IN(100)\n"
                + "  (SUBPARTITION sp0,\n"
                + "   SUBPARTITION sp1,\n"
                + "   SUBPARTITION sp2),\n"
                + " PARTITION p1 VALUES IN(200)\n"
                + "  (SUBPARTITION sp3,\n"
                + "   SUBPARTITION sp4,\n"
                + "   SUBPARTITION sp5)\n"
                + ");";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(2L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.LIST, partition.getPartitionOption().getType());
        Assert.assertEquals(false, partition.getSubpartitionTemplated());
        Assert.assertEquals("col1", partition.getPartitionOption().getColumnNames().get(0));
        Assert.assertEquals("col3", partition.getSubpartition().getPartitionOption().getColumnNames().get(1));
        Assert.assertEquals(DBTablePartitionType.KEY, partition.getSubpartition().getPartitionOption().getType());
        Assert.assertEquals(2, partition.getSubpartition().getPartitionOption().getColumnNames().size());
        Assert.assertTrue(partition.getSubpartition().getPartitionOption().getPartitionsNum() == 3);
    }

    @Test
    public void getPartition_secondary_hash_use_template_Success() {
        String ddl = "CREATE TABLE `range_columns_2_hash_use_template` (\n"
                + "  `col1` int(11) DEFAULT NULL,\n"
                + "  `col2` int(11) DEFAULT NULL\n"
                + ") partition by range columns(col1, col2) subpartition by hash(col2+col1) subpartition "
                + "template (\n"
                + "subpartition p0,\n"
                + "subpartition p1,\n"
                + "subpartition p2,\n"
                + "subpartition p3)\n"
                + "(partition p0 values less than (100,100),\n"
                + "partition p1 values less than (200, 200),\n"
                + "partition p2 values less than (300, 300));";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(3L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE_COLUMNS, partition.getPartitionOption().getType());
        Assert.assertEquals(true, partition.getSubpartitionTemplated());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getSubpartition().getPartitionOption().getType());
        Assert.assertEquals("col2+col1", partition.getSubpartition().getPartitionOption().getExpression());
        Assert.assertTrue(partition.getSubpartition().getPartitionOption().getPartitionsNum() == 4);
    }

    @Test
    public void getPartition_secondary_hash_no_template_Success() {
        String ddl = "CREATE TABLE `range_2_hash_no_template` (col1 INT,col2 INT)\n"
                + "PARTITION BY RANGE(`col1`)\n"
                + "SUBPARTITION BY HASH(`col2`)\n"
                + "(PARTITION p0 VALUES LESS THAN(100)\n"
                + " (SUBPARTITION sp0,\n"
                + "  SUBPARTITION sp1,\n"
                + "  SUBPARTITION sp2),\n"
                + "PARTITION p1 VALUES LESS THAN(200)\n"
                + " (SUBPARTITION sp3,\n"
                + "  SUBPARTITION sp4,\n"
                + "  SUBPARTITION sp5)\n"
                + ");";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        DBTablePartition partition = table.getPartition();
        Assert.assertEquals(2L, partition.getPartitionOption().getPartitionsNum().longValue());
        Assert.assertEquals(DBTablePartitionType.RANGE, partition.getPartitionOption().getType());
        Assert.assertEquals(false, partition.getSubpartitionTemplated());
        Assert.assertEquals(DBTablePartitionType.HASH, partition.getSubpartition().getPartitionOption().getType());
        Assert.assertEquals("col2", partition.getSubpartition().getPartitionOption().getColumnNames().get(0));
        Assert.assertTrue(partition.getSubpartition().getPartitionOption().getPartitionsNum() == 3);
    }

    @Test
    public void getConstraints_test_in_line_constraints_Success() {
        String ddl = "CREATE TABLE `constrains_in_line` (\n"
                + "  `col1` int(11) NOT NULL PRIMARY KEY,\n"
                + "  `col2` int(11) NOT NULL CHECK (`col2`>0),\n"
                + "  `col3` int(11) DEFAULT NULL UNIQUE\n"
                + ")";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        List<DBTableConstraint> constraints = table.listConstraints();
        Assert.assertEquals(5, constraints.size());
    }

    @Test
    public void getConstraints_test_out_line_constraints_Success() {
        String ddl = "CREATE TABLE `constrains_multi` (\n"
                + "  `col1` int(11) NOT NULL,\n"
                + "  `col2` int(11) NOT NULL,\n"
                + "  `col3` int(11) DEFAULT NULL,\n"
                + "  `col4` int(11) DEFAULT NULL,\n"
                + "  `col5` int(11) DEFAULT NULL,\n"
                + "  `col6` int(11) DEFAULT NULL,\n"
                + "  `col7` int(11) DEFAULT NULL,\n"
                + "  PRIMARY KEY (`col1`),\n"
                + "  CONSTRAINT `fk` FOREIGN KEY (`col2`, `col3`) "
                + "REFERENCES `test`.`tab`(`col1`, `col2`) ON UPDATE RESTRICT ON DELETE NO ACTION ,\n"
                + "  UNIQUE KEY `uq2` (`col5`, `col6`) BLOCK_SIZE 16384 LOCAL,\n"
                + "  CONSTRAINT `check` CHECK ((`col7` > 0))\n"
                + ")";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        List<DBTableConstraint> constraints = table.listConstraints();
        Assert.assertEquals(6, constraints.size());
        constraints.forEach(cons -> {
            if ("fk".equals(cons.getName())) {
                Assert.assertEquals(cons.getType(), DBConstraintType.FOREIGN_KEY);
                Assert.assertEquals(cons.getColumnNames().get(1), "col3");
                Assert.assertEquals(cons.getReferenceSchemaName(), "test");
                Assert.assertEquals(cons.getReferenceTableName(), "tab");
                Assert.assertEquals(cons.getReferenceColumnNames().get(0), "col1");
                Assert.assertEquals(cons.getReferenceColumnNames().get(0), "col1");
                Assert.assertEquals(cons.getOnDeleteRule(), DBForeignKeyModifyRule.NO_ACTION);
                Assert.assertEquals(cons.getOnUpdateRule(), DBForeignKeyModifyRule.RESTRICT);
            } else if ("check".equals(cons.getName())) {
                Assert.assertEquals(cons.getType(), DBConstraintType.CHECK);
                Assert.assertEquals("`col7` > 0", cons.getCheckClause());
            } else if ("uq2".equals(cons.getName())) {
                Assert.assertEquals(cons.getType(), DBConstraintType.UNIQUE_KEY);
                Assert.assertEquals(cons.getColumnNames().size(), 2);
            } else if (cons.getType().equals(DBConstraintType.PRIMARY_KEY)) {
                Assert.assertEquals("col1", cons.getColumnNames().get(0));
            }
        });
    }

    @Test
    public void getConstraints_test_out_line_constraints_with_idx_name_Success() {
        String ddl = "create table abcd(\n"
                + "id varchar(64),\n"
                + "constraint `cons_name` unique key `idx_name`(id)\n"
                + ")";
        OBMySQLGetDBTableByParser table = new OBMySQLGetDBTableByParser(ddl);
        List<DBTableConstraint> constraints = table.listConstraints();
        Assert.assertEquals(constraints.get(0).getName(), "idx_name");
        Assert.assertEquals(constraints.get(0).getType(), DBConstraintType.UNIQUE_KEY);
    }
}
