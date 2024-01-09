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

import org.springframework.beans.BeanUtils;

import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.util.StructureCompareUtil;
import com.oceanbase.tools.dbbrowser.editor.DBTableIndexEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableIndex;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
public class DBTableIndexStructureComparator extends AbstractDBObjectStructureComparator<DBTableIndex> {

    private DBTableIndexEditor targetTableIndexEditor;

    public DBTableIndexStructureComparator(DBTableIndexEditor targetTableIndexEditor, String srcSchemaName,
            String tgtSchemaName) {
        super(srcSchemaName, tgtSchemaName);
        this.targetTableIndexEditor = targetTableIndexEditor;
    }

    @Override
    protected DBObjectComparisonResult buildOnlyInTargetResult(DBTableIndex tgtDbObject, String srcSchemaName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.INDEX, tgtDbObject.getName(),
                srcSchemaName, tgtDbObject.getSchemaName());
        result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        result.setChangeScript(StructureCompareUtil
                .appendDelimiterIfNotExist(targetTableIndexEditor.generateDropObjectDDL(tgtDbObject)));
        return result;
    }

    @Override
    protected DBObjectComparisonResult buildOnlyInSourceResult(DBTableIndex srcDbObject, String tgtSchemaName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.INDEX, srcDbObject.getName(),
                srcDbObject.getSchemaName(), tgtSchemaName);
        result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        result.setChangeScript(StructureCompareUtil.appendDelimiterIfNotExist(targetTableIndexEditor
                .generateCreateObjectDDL(copySrcIndexWithTgtSchemaName(srcDbObject, tgtSchemaName))));
        return result;
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
            result.setChangeScript(StructureCompareUtil.appendDelimiterIfNotExist(ddl));
        } else {
            result.setComparisonResult(ComparisonResult.CONSISTENT);
        }
        return result;
    }

    private DBTableIndex copySrcIndexWithTgtSchemaName(DBTableIndex srcIndex, String tgtSchemaName) {
        DBTableIndex copiedSrcIdx = new DBTableIndex();
        BeanUtils.copyProperties(srcIndex, copiedSrcIdx);
        copiedSrcIdx.setSchemaName(tgtSchemaName);
        return copiedSrcIdx;
    }
}
