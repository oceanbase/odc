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
package com.oceanbase.odc.service.connection.logicaldatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBChangeExecutionUnitEntity;
import com.oceanbase.odc.metadb.connection.logicaldatabase.LogicalDBExecutionRepository;
import com.oceanbase.odc.service.connection.logicaldatabase.core.model.LogicalDBChangeExecutionUnit;

/**
 * @Author: Lebie
 * @Date: 2024/9/3 20:09
 * @Description: []
 */
@Service
public class LogicalDatabaseChangeService {
    private final LogicalDatabaseExecutionMapper mapper = LogicalDatabaseExecutionMapper.INSTANCE;

    @Autowired
    private LogicalDBExecutionRepository executionRepository;

    public boolean upsert(List<LogicalDBChangeExecutionUnit> executionUnits) {
        PreConditions.notEmpty(executionUnits, "executionUnits");
        List<LogicalDBChangeExecutionUnitEntity> entities = new ArrayList<>();
        executionUnits.stream().forEach(executionUnit -> {
            LogicalDBChangeExecutionUnitEntity entity;
            Optional<LogicalDBChangeExecutionUnitEntity> opt =
                    executionRepository.findByExecutionId(executionUnit.getExecutionId());
            if (opt.isPresent()) {
                entity = opt.get();
                entity.setExecutionResultJson(JsonUtils.toJson(executionUnit.getExecutionResult()));
            } else {
                entity = mapper.modelToEntity(executionUnit);
            }
            entities.add(entity);
        });
        executionRepository.saveAll(entities);
        return true;
    }
}
