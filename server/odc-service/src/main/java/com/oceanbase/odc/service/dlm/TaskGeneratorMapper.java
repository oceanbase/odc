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

import org.apache.commons.lang.StringUtils;

import com.oceanbase.odc.metadb.dlm.TaskGeneratorEntity;
import com.oceanbase.tools.migrator.common.dto.TaskGenerator;
import com.oceanbase.tools.migrator.common.element.PrimaryKey;
import com.oceanbase.tools.migrator.core.handler.genarator.GeneratorStatus;
import com.oceanbase.tools.migrator.core.handler.genarator.GeneratorType;

/**
 * @Author：tinker
 * @Date: 2023/8/10 15:47
 * @Descripition:
 */
public class TaskGeneratorMapper {


    public static TaskGenerator entityToModel(TaskGeneratorEntity entity) {
        TaskGenerator model = new TaskGenerator();
        model.setId(entity.getGeneratorId());
        model.setJobId(entity.getJobId());
        model.setTaskCount(entity.getTaskCount());
        model.setGeneratorStatus(GeneratorStatus.valueOf(entity.getStatus()));
        model.setGeneratorType(GeneratorType.AUTO);
        model.setProcessedRowCount(entity.getProcessedRowCount());
        model.setProcessedDataSize(entity.getProcessedDataSize());
        if (!StringUtils.isEmpty(entity.getPrimaryKeySavePoint())) {
            model.setGeneratorSavePoint(PrimaryKey.valuesOf(entity.getPrimaryKeySavePoint()));
        }
        model.setGeneratorPartitionSavepoint(entity.getPartitionSavePoint());
        return model;
    }

    public static TaskGeneratorEntity modelToEntity(TaskGenerator model) {
        TaskGeneratorEntity entity = new TaskGeneratorEntity();
        entity.setJobId(model.getJobId());
        entity.setType(GeneratorType.AUTO.name());
        entity.setGeneratorId(model.getId());
        entity.setTaskCount(model.getTaskCount());
        entity.setStatus(model.getGeneratorStatus().name());
        entity.setPartitionSavePoint(model.getGeneratorPartitionSavepoint());
        entity.setProcessedDataSize(model.getProcessedDataSize());
        entity.setProcessedRowCount(model.getProcessedRowCount());
        if (model.getGeneratorSavePoint() != null) {
            entity.setPrimaryKeySavePoint(model.getGeneratorSavePoint().toSqlString());
        }
        return entity;
    }

}
