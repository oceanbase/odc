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
package com.oceanbase.odc.service.dlm;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.metadb.dlm.TaskUnitEntity;
import com.oceanbase.tools.migrator.common.element.PrimaryKey;
import com.oceanbase.tools.migrator.common.enums.TaskStatus;
import com.oceanbase.tools.migrator.core.meta.TaskMeta;

/**
 * @Author：tinker
 * @Date: 2023/8/10 17:26
 * @Descripition:
 */
public class TaskUnitMapper {


    public static TaskMeta entityToModel(TaskUnitEntity entity) {
        TaskMeta taskMeta = new TaskMeta();
        taskMeta.setGeneratorId(entity.getGeneratorId());
        taskMeta.setTaskIndex(entity.getTaskIndex());
        taskMeta.setTaskStatus(TaskStatus.valueOf(entity.getStatus()));
        taskMeta.setPartitionName(entity.getPartitionName());
        if (StringUtils.isNotEmpty(entity.getLowerBoundPrimaryKey())) {
            taskMeta.setMinPrimaryKey(PrimaryKey.valuesOf(entity.getLowerBoundPrimaryKey()));
        }
        if (StringUtils.isNotEmpty(entity.getUpperBoundPrimaryKey())) {
            taskMeta.setMaxPrimaryKey(PrimaryKey.valuesOf(entity.getUpperBoundPrimaryKey()));
        }
        if (StringUtils.isNotEmpty(entity.getPrimaryKeyCursor())) {
            taskMeta.setCursorPrimaryKey(PrimaryKey.valuesOf(entity.getPrimaryKeyCursor()));
        }
        return taskMeta;
    }

    public static TaskUnitEntity modelToEntity(TaskMeta model) {
        TaskUnitEntity entity = new TaskUnitEntity();
        entity.setStatus(model.getTaskStatus().name());
        entity.setGeneratorId(model.getGeneratorId());
        entity.setTaskIndex(model.getTaskIndex());
        entity.setJobId(model.getJobMeta().getJobId());
        entity.setPartitionName(model.getPartitionName());
        if (model.getMaxPrimaryKey() != null) {
            entity.setUpperBoundPrimaryKey(model.getMaxPrimaryKey().toSqlString());
        }
        if (model.getMinPrimaryKey() != null) {
            entity.setLowerBoundPrimaryKey(model.getMinPrimaryKey().toSqlString());
        }
        if (model.getCursorPrimaryKey() != null) {
            entity.setPrimaryKeyCursor(model.getCursorPrimaryKey().toSqlString());
        }
        return entity;
    }
}
