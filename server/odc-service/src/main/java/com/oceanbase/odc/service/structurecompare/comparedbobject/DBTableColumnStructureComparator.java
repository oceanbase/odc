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

import javax.validation.constraints.NotEmpty;

import org.springframework.beans.BeanUtils;

import com.oceanbase.odc.service.structurecompare.model.ComparisonResult;
import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.odc.service.structurecompare.util.StructureCompareUtil;
import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
public class DBTableColumnStructureComparator extends AbstractDBObjectStructureComparator<DBTableColumn> {
    private DBTableColumnEditor tgtColumnEditor;

    public DBTableColumnStructureComparator(DBTableColumnEditor tgtColumnEditor, String srcSchemaName,
            String tgtSchemaName) {
        super(srcSchemaName, tgtSchemaName);
        this.tgtColumnEditor = tgtColumnEditor;
    }

    @Override
    public List<DBObjectComparisonResult> compare(@NotEmpty List<DBTableColumn> srcTabCols,
            @NotEmpty List<DBTableColumn> tgtTabCols) {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();

        String srcSchemaName = srcTabCols.get(0).getSchemaName();
        String tgtSchemaName = tgtTabCols.get(0).getSchemaName();
        List<String> srcColNames = srcTabCols.stream().map(DBTableColumn::getName).collect(Collectors.toList());
        List<String> tgtColNames = tgtTabCols.stream().map(DBTableColumn::getName).collect(Collectors.toList());
        Map<String, DBTableColumn> srcColumnName2Column =
                srcTabCols.stream().collect(Collectors.toMap(DBTableColumn::getName, col -> col));
        Map<String, DBTableColumn> tgtColumnName2Column =
                tgtTabCols.stream().collect(Collectors.toMap(DBTableColumn::getName, col -> col));

        tgtColNames.forEach(tarColName -> {
            if (!srcColNames.contains(tarColName)) {
                // column to be dropped
                returnVal.add(buildOnlyInTargetResult(tgtColumnName2Column.get(tarColName), srcSchemaName));
            }
        });

        srcColNames.forEach(srcColName -> {
            if (tgtColNames.contains(srcColName)) {
                // column to be compared
                returnVal.add(compare(srcColumnName2Column.get(srcColName), tgtColumnName2Column.get(srcColName)));
            } else {
                // column to be created
                returnVal.add(buildOnlyInSourceResult(srcColumnName2Column.get(srcColName), tgtSchemaName));
            }
        });

        return returnVal;
    }

    @Override
    protected DBObjectComparisonResult buildOnlyInTargetResult(DBTableColumn tgtDbObject, String srcSchemaName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.COLUMN, tgtDbObject.getName(),
                srcSchemaName, tgtSchemaName);
        result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        result.setChangeScript(
                StructureCompareUtil.appendDelimiterIfNotExist(
                        this.tgtColumnEditor.generateDropObjectDDL(tgtDbObject)));
        return result;
    }

    @Override
    protected DBObjectComparisonResult buildOnlyInSourceResult(DBTableColumn srcDbObject, String tgtSchemaName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.COLUMN, srcDbObject.getName(),
                srcSchemaName, tgtSchemaName);
        result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
        result.setChangeScript(StructureCompareUtil
                .appendDelimiterIfNotExist(tgtColumnEditor
                        .generateCreateObjectDDL(copySrcColumnWithTgtSchemaName(srcDbObject, tgtSchemaName))));
        return result;
    }

    @Override
    public DBObjectComparisonResult compare(@NonNull DBTableColumn srcTabCol, @NonNull DBTableColumn tgtTabCol) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.COLUMN, srcTabCol.getName(),
                srcTabCol.getSchemaName(), tgtTabCol.getSchemaName());

        DBTableColumn copiedSrcCol = copySrcColumnWithTgtSchemaName(srcTabCol, tgtTabCol.getSchemaName());

        String ddl = this.tgtColumnEditor.generateUpdateObjectDDL(
                tgtTabCol, copiedSrcCol);
        if (!ddl.isEmpty()) {
            // column to be updated
            result.setComparisonResult(ComparisonResult.INCONSISTENT);
            result.setChangeScript(StructureCompareUtil.appendDelimiterIfNotExist(ddl));
        } else {
            result.setComparisonResult(ComparisonResult.CONSISTENT);
        }
        return result;
    }

    private DBTableColumn copySrcColumnWithTgtSchemaName(DBTableColumn srcCol, String tgtSchemaName) {
        DBTableColumn copiedSrcCol = new DBTableColumn();
        BeanUtils.copyProperties(srcCol, copiedSrcCol);
        copiedSrcCol.setSchemaName(tgtSchemaName);
        return copiedSrcCol;
    }
}
