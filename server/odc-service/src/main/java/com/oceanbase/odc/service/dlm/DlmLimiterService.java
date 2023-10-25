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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.dlm.DlmLimiterConfigEntity;
import com.oceanbase.odc.metadb.dlm.DlmLimiterConfigRepository;
import com.oceanbase.odc.service.dlm.model.DlmLimiterConfig;

/**
 * @Author：tinker
 * @Date: 2023/8/3 14:06
 * @Descripition:
 */
@Service
@SkipAuthorize("odc internal usage")
public class DlmLimiterService {

    @Value("${odc.task.dlm.default-single-task-row-limit:20000}")
    private int defaultRowLimit;

    @Value("${odc.task.dlm.max-single-task-row-limit:50000}")
    private int maxRowLimit;

    @Value("${odc.task.dlm.default-single-task-data-size-limit:1024}")
    private long defaultDataSizeLimit;

    @Value("${odc.task.dlm.max-single-task-data-size-limit:10240}")
    private long maxDataSizeLimit;

    @Value("${odc.task.dlm.default-single-thread-batch-size:200}")
    private int defaultBatchSize;

    private final DlmLimiterConfigMapper mapper = DlmLimiterConfigMapper.INSTANCE;

    @Autowired
    private DlmLimiterConfigRepository limiterConfigRepository;

    public DlmLimiterConfigEntity createAndBindToOrder(Long orderId, DlmLimiterConfig config) {
        checkLimiterConfig(config);
        DlmLimiterConfigEntity entity = mapper.modelToEntity(config);
        entity.setOrderId(orderId);
        return limiterConfigRepository.save(entity);
    }

    public DlmLimiterConfig getByOrderIdOrElseDefaultConfig(Long orderId) {
        Optional<DlmLimiterConfigEntity> entityOptional = limiterConfigRepository.findByOrderId(orderId);
        if (entityOptional.isPresent()) {
            return mapper.entityToModel(entityOptional.get());
        } else {
            return getDefaultLimiterConfig();
        }
    }

    public DlmLimiterConfig updateByOrderId(Long orderId, DlmLimiterConfig limiterConfig) {
        checkLimiterConfig(limiterConfig);
        Optional<DlmLimiterConfigEntity> entityOptional = limiterConfigRepository.findByOrderId(orderId);
        if (entityOptional.isPresent()) {
            DlmLimiterConfigEntity entity = entityOptional.get();
            entity.setRowLimit(
                    limiterConfig.getRowLimit() == null ? entity.getRowLimit() : limiterConfig.getRowLimit());
            entity.setBatchSize(
                    limiterConfig.getBatchSize() == null ? entity.getBatchSize() : limiterConfig.getBatchSize());
            entity.setDataSizeLimit(limiterConfig.getDataSizeLimit() == null ? entity.getDataSizeLimit()
                    : limiterConfig.getDataSizeLimit());
            return mapper.entityToModel(limiterConfigRepository.save(entity));
        } else {
            throw new NotFoundException(ResourceType.ODC_DLM_LIMITER_CONFIG, "Id", orderId);
        }
    }

    public DlmLimiterConfig getDefaultLimiterConfig() {
        DlmLimiterConfig dlmLimiterConfig = new DlmLimiterConfig();
        dlmLimiterConfig.setRowLimit(defaultRowLimit);
        dlmLimiterConfig.setDataSizeLimit(defaultDataSizeLimit);
        dlmLimiterConfig.setBatchSize(defaultBatchSize);
        return dlmLimiterConfig;
    }

    private void checkLimiterConfig(DlmLimiterConfig limiterConfig) {
        if (limiterConfig.getRowLimit() != null && limiterConfig.getRowLimit() > maxRowLimit) {
            throw new IllegalArgumentException(String.format("The maximum row limit is %s rows/s.", maxRowLimit));
        }
        if (limiterConfig.getDataSizeLimit() != null && limiterConfig.getDataSizeLimit() > maxDataSizeLimit) {
            throw new IllegalArgumentException(String.format("The maximum data size is %s KB/s.", maxDataSizeLimit));
        }
    }
}
