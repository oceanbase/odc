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
public class DBTableConstraintStructureComparator extends AbstractDBObjectStructureComparator<DBTableConstraint> {
    private DBTableConstraintEditor tgtConstraintEditor;

    public DBTableConstraintStructureComparator(DBTableConstraintEditor tgtConstraintEditor, String srcSchemaName,
            String tgtSchemaName, String srcTableName, String tgtTableName) {
        super(srcSchemaName, tgtSchemaName, srcTableName, tgtTableName);
        this.tgtConstraintEditor = tgtConstraintEditor;
    }

    @Override
    protected DBObjectComparisonResult buildOnlyInTargetResult(DBTableConstraint tgtDbObject, String srcSchemaName,
            String srcTableName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.CONSTRAINT, tgtDbObject.getName(),
                srcSchemaName, tgtDbObject.getSchemaName());
        result.setComparisonResult(ComparisonResult.ONLY_IN_TARGET);
        result.setChangeScript(this.tgtConstraintEditor.generateDropObjectDDL(tgtDbObject));
        return result;
    }

    @Override
    protected DBObjectComparisonResult buildOnlyInSourceResult(DBTableConstraint srcDbObject, String tgtSchemaName,
            String tgtTableName) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.CONSTRAINT, srcDbObject.getName(),
                srcDbObject.getSchemaName(), tgtSchemaName);
        result.setComparisonResult(ComparisonResult.ONLY_IN_SOURCE);

        DBTableConstraint copiedSrcCons =
                copySrcConstraintWithTgtSchemaNameAndTgtTableName(srcDbObject, tgtSchemaName, tgtTableName);
        if (copiedSrcCons.getType().equals(DBConstraintType.FOREIGN_KEY)) {
            copiedSrcCons.setReferenceSchemaName(tgtSchemaName);
        }
        result.setChangeScript(this.tgtConstraintEditor.generateCreateObjectDDL(copiedSrcCons));
        return result;
    }

    private DBTableConstraint copySrcConstraintWithTgtSchemaNameAndTgtTableName(DBTableConstraint srcConstraint,
            String tgtSchemaName, String tgtTableName) {
        DBTableConstraint copiedSrcConstraint = new DBTableConstraint();
        BeanUtils.copyProperties(srcConstraint, copiedSrcConstraint);
        copiedSrcConstraint.setSchemaName(tgtSchemaName);
        copiedSrcConstraint.setTableName(tgtTableName);
        return copiedSrcConstraint;
    }

    @Override
    public DBObjectComparisonResult compare(@NonNull DBTableConstraint srcConstraint,
            @NonNull DBTableConstraint tgtConstraint) {
        DBObjectComparisonResult result = new DBObjectComparisonResult(DBObjectType.CONSTRAINT, srcConstraint.getName(),
                this.srcSchemaName, this.tgtSchemaName);

        DBTableConstraint copiedSrcConstraint = copySrcConstraintWithTgtSchemaNameAndTgtTableName(srcConstraint,
                this.tgtSchemaName, tgtConstraint.getTableName());

        String ddl = this.tgtConstraintEditor.generateUpdateObjectDDL(
                tgtConstraint, copiedSrcConstraint);
        if (!ddl.isEmpty()) {
            // constraint to be updated
            result.setComparisonResult(ComparisonResult.INCONSISTENT);
            result.setChangeScript(ddl);
        } else {
            result.setComparisonResult(ComparisonResult.CONSISTENT);
        }
        return result;
    }

}
