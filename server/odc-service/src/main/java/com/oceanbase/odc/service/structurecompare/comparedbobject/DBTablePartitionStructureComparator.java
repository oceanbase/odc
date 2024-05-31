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
package com.oceanbase.odc.service.structurecompare.comparedbobject;

import java.util.List;

import org.springframework.beans.BeanUtils;

import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.tools.dbbrowser.editor.DBTablePartitionEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTablePartition;
import com.oceanbase.tools.dbbrowser.model.DBTablePartitionType;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
public class DBTablePartitionStructureComparator implements DBObjectStructureComparator<DBTablePartition> {
    private DBTablePartitionEditor tgtPartitionEditor;
    private String srcSchemaName;
    private String tgtSchemaName;
    private String srcTableName;
    private String tgtTableName;

    public DBTablePartitionStructureComparator(DBTablePartitionEditor tgtPartitionEditor, String srcSchemaName,
            String tgtSchemaName, String srcTableName, String tgtTableName) {
        this.tgtPartitionEditor = tgtPartitionEditor;
        this.srcSchemaName = srcSchemaName;
        this.tgtSchemaName = tgtSchemaName;
        this.srcTableName = srcTableName;
        this.tgtTableName = tgtTableName;
    }

    @Override
    public List<DBObjectComparisonResult> compare(List<DBTablePartition> srcPartitions,
            List<DBTablePartition> tgtPartitions) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public DBObjectComparisonResult compare(DBTablePartition srcPartition, DBTablePartition tgtPartition) {
        DBTablePartitionType srcPartitionType = srcPartition.getPartitionOption().getType();
        DBTablePartitionType tgtPartitionType = tgtPartition.getPartitionOption().getType();
        DBObjectComparisonResult result =
                new DBObjectComparisonResult(DBObjectType.PARTITION, this.srcSchemaName, this.tgtSchemaName);

        if (DBTablePartitionType.NOT_PARTITIONED.equals(srcPartitionType)
                && DBTablePartitionType.NOT_PARTITIONED.equals(tgtPartitionType)) {
            result.setComparisonResult(ComparisonResult.CONSISTENT);
        } else {
            String ddl = this.tgtPartitionEditor.generateShadowTableUpdateObjectDDL(tgtPartition,
                    copySrcPartitionWithTgtSchemaNameAndTgtTableName(srcPartition, this.tgtSchemaName,
                            this.tgtTableName));
            if (ddl.isEmpty()) {
                result.setComparisonResult(ComparisonResult.CONSISTENT);
            } else {
                // partition to be updated
                result.setComparisonResult(ComparisonResult.INCONSISTENT);
                result.setChangeScript(ddl);
            }
        }
        return result;
    }

    private DBTablePartition copySrcPartitionWithTgtSchemaNameAndTgtTableName(DBTablePartition srcPartition,
            String tgtSchemaName, String tgtTableName) {
        DBTablePartition copiedSrcPartition = new DBTablePartition();
        BeanUtils.copyProperties(srcPartition, copiedSrcPartition);
        copiedSrcPartition.setSchemaName(tgtSchemaName);
        copiedSrcPartition.setTableName(tgtTableName);
        return copiedSrcPartition;
    }
}
