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
package com.oceanbase.odc.service.structurecompare;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

/**
 * @author jingtian
 * @date 2024/1/19
 * @since ODC_release_4.2.4
 */
public class DBObjectComparisonResultTest {

    @Test
    public void testToEntity_putDropStatementInComments_success() {
        String actual = buildResult().toEntity(1L, DialectType.OB_MYSQL).getChangeSqlScript();
        String expected = "-- Unsupported operation to drop primary key constraint\n"
                + "\n"
                + "/*\n"
                + "ALTER TABLE `tgtSchema`.`t1` DROP COLUMN `c1`;\n"
                + "*/\n"
                + "\n"
                + "/*\n"
                + "ALTER TABLE `tgtSchema`.`t1` DROP PARTITION (p1);\n"
                + "*/\n"
                + "\n"
                + "/*\n"
                + "ALTER TABLE `tgtSchema`.`t1` DROP PARTITION (p2);\n"
                + "*/\n"
                + "\n"
                + "ALTER TABLE `tgtSchema`.`t1` ADD PARTITION (p2);\n"
                + "\n"
                + "ALTER TABLE `tgtSchema`.`t1` COMMENT = 'comment1';\n"
                + "-- Unsupported operation to modify table charset\n";
        Assert.assertEquals(expected, actual);

    }

    private DBObjectComparisonResult buildResult() {
        DBObjectComparisonResult unsupported =
                new DBObjectComparisonResult(DBObjectType.CONSTRAINT, "PRIMARY", "srcSchema", "tgtSchema");
        unsupported.setChangeScript("-- Unsupported operation to drop primary key constraint\n");
        unsupported.setComparisonResult(ComparisonResult.INCONSISTENT);

        DBObjectComparisonResult dropColumn =
                new DBObjectComparisonResult(DBObjectType.COLUMN, "c1", "srcSchema", "tgtSchema");
        dropColumn.setChangeScript("ALTER TABLE `tgtSchema`.`t1` DROP COLUMN `c1`;\n");
        dropColumn.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);

        DBObjectComparisonResult dropPartition =
                new DBObjectComparisonResult(DBObjectType.PARTITION, "srcSchema", "tgtSchema");
        dropPartition.setComparisonResult(ComparisonResult.INCONSISTENT);
        dropPartition.setChangeScript("ALTER TABLE `tgtSchema`.`t1` DROP PARTITION (p1);\n"
                + "ALTER TABLE `tgtSchema`.`t1` DROP PARTITION (p2);\n");

        DBObjectComparisonResult addPartition =
                new DBObjectComparisonResult(DBObjectType.PARTITION, "srcSchema", "tgtSchema");
        addPartition.setComparisonResult(ComparisonResult.INCONSISTENT);
        addPartition.setChangeScript("ALTER TABLE `tgtSchema`.`t1` ADD PARTITION (p2);\n");

        DBObjectComparisonResult returnVal = new DBObjectComparisonResult(DBObjectType.TABLE, "srcSchema", "tgtSchema");
        returnVal.setComparisonResult(ComparisonResult.INCONSISTENT);
        returnVal.setChangeScript("ALTER TABLE `tgtSchema`.`t1` COMMENT = 'comment1';\n"
                + "-- Unsupported operation to modify table charset\n");
        returnVal.setSubDBObjectComparisonResult(Arrays.asList(unsupported, dropColumn, dropPartition, addPartition));
        return returnVal;
    }
}
