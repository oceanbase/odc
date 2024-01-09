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

import com.oceanbase.odc.service.structurecompare.model.DBObjectComparisonResult;
import com.oceanbase.tools.dbbrowser.model.DBObject;

/**
 * @author jingtian
 * @date 2024/1/9
 * @since ODC_release_4.2.4
 */
public abstract class AbstractDBObjectStructureComparator<T extends DBObject>
        implements DBObjectStructureComparator<T> {
    protected final String srcSchemaName;
    protected final String tgtSchemaName;

    public AbstractDBObjectStructureComparator(String srcSchemaName, String tgtSchemaName) {
        this.srcSchemaName = srcSchemaName;
        this.tgtSchemaName = tgtSchemaName;
    }

    @Override
    public List<DBObjectComparisonResult> compare(List<T> sourceObjects, List<T> targetObjects) {
        List<DBObjectComparisonResult> returnVal = new ArrayList<>();
        if (sourceObjects.isEmpty() && targetObjects.isEmpty()) {
            return returnVal;
        } else if (sourceObjects.isEmpty()) {
            // database objects to be dropped
            targetObjects.forEach(object -> {
                returnVal.add(buildOnlyInTargetResult(object, this.srcSchemaName));
            });
            return returnVal;
        } else if (targetObjects.isEmpty()) {
            // database objects to be created
            sourceObjects.forEach(object -> {
                returnVal.add(buildOnlyInSourceResult(object, this.tgtSchemaName));
            });
            return returnVal;
        }

        List<String> srcObjectNames = sourceObjects.stream().map(DBObject::name).collect(Collectors.toList());
        List<String> tgtObjectNames = targetObjects.stream().map(DBObject::name).collect(Collectors.toList());
        Map<String, T> srcObjectName2Object =
                sourceObjects.stream().collect(Collectors.toMap(DBObject::name, object -> object));
        Map<String, T> tgtObjectName2Object =
                targetObjects.stream().collect(Collectors.toMap(DBObject::name, object -> object));

        tgtObjectNames.forEach(tgtObjectName -> {
            if (!srcObjectNames.contains(tgtObjectName)) {
                // database object to be dropped
                returnVal.add(buildOnlyInTargetResult(tgtObjectName2Object.get(tgtObjectName), this.srcSchemaName));
            } else {
                // database object to be compared
                returnVal
                        .add(compare(srcObjectName2Object.get(tgtObjectName), tgtObjectName2Object.get(tgtObjectName)));
            }
        });

        srcObjectNames.forEach(srcObjectName -> {
            if (!tgtObjectNames.contains(srcObjectName)) {
                // database object to be created
                returnVal.add(buildOnlyInSourceResult(srcObjectName2Object.get(srcObjectName), this.tgtSchemaName));
            }
        });

        return returnVal;
    }

    protected abstract DBObjectComparisonResult buildOnlyInTargetResult(T tgtDbObject, String srcSchemaName);

    protected abstract DBObjectComparisonResult buildOnlyInSourceResult(T srcDbObject, String tgtSchemaName);
}
