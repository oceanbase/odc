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
import com.oceanbase.tools.dbbrowser.editor.DBTableColumnEditor;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
public class TableColumnStructureComparator implements DBObjectStructureComparator<DBTableColumn> {
    private DBTableColumnEditor tgtColumnEditor;
    private String srcSchemaName;
    private String tgtSchemaName;

    public TableColumnStructureComparator(DBTableColumnEditor tgtColumnEditor, String srcSchemaName,
            String tgtSchemaName) {
        this.tgtColumnEditor = tgtColumnEditor;
        this.srcSchemaName = srcSchemaName;
        this.tgtSchemaName = tgtSchemaName;
    }

    @Override
    public List<DBObjectComparisonResult> compare(@NotEmpty List<DBTableColumn> srcTabCols,
            @NotEmpty List<DBTableColumn> tgtTabCols) {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();

        String srcSchemaName = srcTabCols.get(0).getSchemaName();
        String tgtSchemaName = tgtTabCols.get(0).getSchemaName();
        List<String> srcColNames = srcTabCols.stream().map(DBTableColumn::getName).collect(Collectors.toList());
        List<String> tgtColNames = tgtTabCols.stream().map(DBTableColumn::getName).collect(Collectors.toList());
        Map<String, DBTableColumn> srcColMapping =
                srcTabCols.stream().collect(Collectors.toMap(DBTableColumn::getName, col -> col));
        Map<String, DBTableColumn> tgtColMapping =
                tgtTabCols.stream().collect(Collectors.toMap(DBTableColumn::getName, col -> col));

        tgtColNames.forEach(tarColName -> {
            if (!srcColNames.contains(tarColName)) {
                // column to be dropped
                DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.COLUMN, tarColName,
                        srcSchemaName, tgtSchemaName);
                result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
                result.setChangeScript(
                        appendDelimiterIfNotExist(this.tgtColumnEditor
                                .generateDropObjectDDL(tgtColMapping.get(tarColName))));
                returnVal.add(result);
            }
        });

        srcColNames.forEach(srcColName -> {
            DBTableColumn copiedSrcCol = copySrcColumnWithTgtSchemaName(srcColMapping.get(srcColName), tgtSchemaName);

            if (tgtColNames.contains(srcColName)) {
                // column to be compared
                returnVal.add(compare(srcColMapping.get(srcColName), tgtColMapping.get(srcColName)));
            } else {
                // column to be created
                DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.COLUMN, srcColName,
                        srcSchemaName, tgtSchemaName);
                result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);
                result.setChangeScript(appendDelimiterIfNotExist(
                        tgtColumnEditor.generateCreateObjectDDL(copiedSrcCol)));
                returnVal.add(result);
            }
        });

        return returnVal;
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
            result.setChangeScript(appendDelimiterIfNotExist(ddl));
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
