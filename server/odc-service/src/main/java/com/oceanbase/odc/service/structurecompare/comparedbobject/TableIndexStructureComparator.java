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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;

import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
public class TableIndexStructureComparator implements DBObjectStructureComparator<DBTableIndex> {

    private DBTableIndexEditor targetTableIndexEditor;
    private String srcSchemaName;
    private String tgtSchemaName;

    public TableIndexStructureComparator(DBTableIndexEditor targetTableIndexEditor, String srcSchemaName,
            String tgtSchemaName) {
        this.targetTableIndexEditor = targetTableIndexEditor;
        this.srcSchemaName = srcSchemaName;
        this.tgtSchemaName = tgtSchemaName;
    }

    @Override
    public List<DBObjectComparisonResult> compare(List<DBTableIndex> srcIndexes, List<DBTableIndex> tgtIndexes) {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        if (srcIndexes.isEmpty() && tgtIndexes.isEmpty()) {
            return returnVal;
        } else if (srcIndexes.isEmpty()) {
            // indexes to be dropped
            tgtIndexes.forEach(idx -> {
                returnVal.add(buildDropIndexResult(idx, this.srcSchemaName));
            });
            return returnVal;
        } else if (tgtIndexes.isEmpty()) {
            // indexes to be created
            srcIndexes.forEach(idx -> {
                returnVal.add(buildCreateIndexResult(idx, this.tgtSchemaName));
            });
            return returnVal;
        }

        List<String> srcIdxNames = srcIndexes.stream().map(DBTableIndex::getName).collect(Collectors.toList());
        List<String> tgtIdxNames = tgtIndexes.stream().map(DBTableIndex::getName).collect(Collectors.toList());
        Map<String, DBTableIndex> srcIdxMapping =
                srcIndexes.stream().collect(Collectors.toMap(DBTableIndex::getName, col -> col));
        Map<String, DBTableIndex> tgtIdxMapping =
                tgtIndexes.stream().collect(Collectors.toMap(DBTableIndex::getName, col -> col));

        tgtIdxNames.forEach(tgtIdxName -> {
            if (!srcIdxNames.contains(tgtIdxName)) {
                // index to be dropped
                returnVal.add(buildDropIndexResult(tgtIdxMapping.get(tgtIdxName), this.srcSchemaName));
            } else {
                // index to be compared
                returnVal.add(compare(srcIdxMapping.get(tgtIdxName), tgtIdxMapping.get(tgtIdxName)));
            }
        });

        srcIdxNames.forEach(srcIdxName -> {
            if (!tgtIdxNames.contains(srcIdxName)) {
                // index to be created
                returnVal.add(buildCreateIndexResult(srcIdxMapping.get(srcIdxName), this.tgtSchemaName));
            }
        });

        return returnVal;
    }

    @Override
    public DBObjectComparisonResult compare(@NonNull DBTableIndex srcIndex, @NonNull DBTableIndex tgtIndex) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.INDEX, srcIndex.getName(),
                srcIndex.getSchemaName(), tgtIndex.getSchemaName());

        DBTableIndex copiedSrcIdx = copySrcIndexWithTgtSchemaName(srcIndex, tgtIndex.getSchemaName());
        String ddl = this.targetTableIndexEditor.generateUpdateObjectDDL(
                tgtIndex, copiedSrcIdx);
        if (!ddl.isEmpty()) {
            // index to be updated
            result.setComparisonResult(ComparisonResult.INCONSISTENT);
            result.setChangeScript(appendDelimiterIfNotExist(ddl));
        } else {
            result.setComparisonResult(ComparisonResult.CONSISTENT);
        }
        return result;
    }

    private DBObjectComparisonResult buildCreateIndexResult(DBTableIndex srcIndex, String tgtSchemaName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.INDEX, srcIndex.getName(),
                srcIndex.getSchemaName(), tgtSchemaName);
        result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        result.setChangeScript(appendDelimiterIfNotExist(
                targetTableIndexEditor
                        .generateCreateObjectDDL(copySrcIndexWithTgtSchemaName(srcIndex, tgtSchemaName))));
        return result;
    }

    private DBObjectComparisonResult buildDropIndexResult(DBTableIndex tgtIndex, String srcSchemaName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.INDEX, tgtIndex.getName(),
                srcSchemaName, tgtIndex.getSchemaName());
        result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        result.setChangeScript(appendDelimiterIfNotExist(
                targetTableIndexEditor.generateDropObjectDDL(tgtIndex)));
        return result;
    }

    private DBTableIndex copySrcIndexWithTgtSchemaName(DBTableIndex srcIndex, String tgtSchemaName) {
        DBTableIndex copiedSrcIdx = new DBTableIndex();
        BeanUtils.copyProperties(srcIndex, copiedSrcIdx);
        copiedSrcIdx.setSchemaName(tgtSchemaName);
        return copiedSrcIdx;
    }
}
