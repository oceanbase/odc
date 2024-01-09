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
import com.oceanbase.tools.dbbrowser.editor.DBTableConstraintEditor;
import com.oceanbase.tools.dbbrowser.model.DBConstraintType;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;
import com.oceanbase.tools.dbbrowser.model.DBTableConstraint;

import lombok.NonNull;

/**
 * @author jingtian
 * @date 2024/1/4
 * @since ODC_release_4.2.4
 */
public class TableConstraintStructureComparator implements DBObjectStructureComparator<DBTableConstraint> {
    private DBTableConstraintEditor tgtConstraintEditor;
    private String srcSchemaName;
    private String tgtSchemaName;

    public TableConstraintStructureComparator(DBTableConstraintEditor tgtConstraintEditor, String srcSchemaName,
            String tgtSchemaName) {
        this.tgtConstraintEditor = tgtConstraintEditor;
        this.srcSchemaName = srcSchemaName;
        this.tgtSchemaName = tgtSchemaName;
    }

    @Override
    public List<DBObjectComparisonResult> compare(List<DBTableConstraint> srcTabCons,
            List<DBTableConstraint> tgtTabCons) {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        if (srcTabCons.isEmpty() && tgtTabCons.isEmpty()) {
            return returnVal;
        } else if (srcTabCons.isEmpty()) {
            // constraints to be dropped
            tgtTabCons.forEach(cons -> {
                returnVal.add(buildDropConstraintResult(cons, this.srcSchemaName));
            });
            return returnVal;
        } else if (tgtTabCons.isEmpty()) {
            // constraints to be created
            srcTabCons.forEach(cons -> {
                returnVal.add(buildCreateConstraintResult(cons, this.tgtSchemaName));
            });
            return returnVal;
        }

        List<String> srcConsNames = srcTabCons.stream().map(DBTableConstraint::getName).collect(Collectors.toList());
        List<String> tgtConsNames = tgtTabCons.stream().map(DBTableConstraint::getName).collect(Collectors.toList());
        Map<String, DBTableConstraint> srcConsMapping =
                srcTabCons.stream().collect(Collectors.toMap(DBTableConstraint::getName, col -> col));
        Map<String, DBTableConstraint> tarConsMapping =
                tgtTabCons.stream().collect(Collectors.toMap(DBTableConstraint::getName, col -> col));

        tgtConsNames.forEach(tgtConsName -> {
            if (!srcConsNames.contains(tgtConsName)) {
                // constraint to be dropped
                returnVal.add(buildDropConstraintResult(tarConsMapping.get(tgtConsName), this.srcSchemaName));
            } else {
                // constraint to be compared
                returnVal.add(compare(srcConsMapping.get(tgtConsName), tarConsMapping.get(tgtConsName)));
            }
        });

        srcConsNames.forEach(srcConsName -> {
            if (!tgtConsNames.contains(srcConsName)) {
                // constraint to be created
                returnVal.add(buildCreateConstraintResult(srcConsMapping.get(srcConsName), this.tgtSchemaName));
            }
        });

        return returnVal;
    }

    private DBObjectComparisonResult buildDropConstraintResult(DBTableConstraint tgtCons, String srcSchemaName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.CONSTRAINT, tgtCons.getName(),
                srcSchemaName, tgtCons.getSchemaName());
        result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        result.setChangeScript(appendDelimiterIfNotExist(
                this.tgtConstraintEditor.generateDropObjectDDL(tgtCons)));
        return result;
    }

    private DBObjectComparisonResult buildCreateConstraintResult(DBTableConstraint srcConstraint,
            String tgtSchemaName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.CONSTRAINT, srcConstraint.getName(),
                srcConstraint.getSchemaName(), tgtSchemaName);
        result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);

        DBTableConstraint copiedSrcCons = copySrcConstraintWithTgtSchemaName(srcConstraint, tgtSchemaName);
        if (copiedSrcCons.getType().equals(DBConstraintType.FOREIGN_KEY)) {
            copiedSrcCons.setReferenceSchemaName(tgtSchemaName);
        }
        result.setChangeScript(appendDelimiterIfNotExist(
                this.tgtConstraintEditor
                        .generateCreateObjectDDL(copiedSrcCons)));
        return result;
    }

    private DBTableConstraint copySrcConstraintWithTgtSchemaName(DBTableConstraint srcConstraint,
            String tgtSchemaName) {
        DBTableConstraint copiedSrcConstraint = new DBTableConstraint();
        BeanUtils.copyProperties(srcConstraint, copiedSrcConstraint);
        copiedSrcConstraint.setSchemaName(tgtSchemaName);
        return copiedSrcConstraint;
    }

    @Override
    public DBObjectComparisonResult compare(@NonNull DBTableConstraint srcConstraint,
            @NonNull DBTableConstraint tgtConstraint) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.CONSTRAINT, srcConstraint.getName(),
                this.srcSchemaName, this.tgtSchemaName);

        DBTableConstraint copiedSrcConstraint = copySrcConstraintWithTgtSchemaName(srcConstraint, this.tgtSchemaName);
        String ddl = this.tgtConstraintEditor.generateUpdateObjectDDL(
                tgtConstraint, copiedSrcConstraint);
        if (!ddl.isEmpty()) {
            // constraint to be updated
            result.setComparisonResult(ComparisonResult.INCONSISTENT);
            result.setChangeScript(appendDelimiterIfNotExist(ddl));
        } else {
            result.setComparisonResult(ComparisonResult.CONSISTENT);
        }
        return result;
    }
}
