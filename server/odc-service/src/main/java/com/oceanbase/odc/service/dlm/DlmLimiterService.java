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
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.dlm.DlmLimiterConfigEntity;
import com.oceanbase.odc.metadb.dlm.DlmLimiterConfigRepository;
import com.oceanbase.odc.service.dlm.model.DataArchiveParameters;
import com.oceanbase.odc.service.dlm.model.RateLimitConfiguration;
import com.oceanbase.odc.service.schedule.ScheduleService;
import com.oceanbase.odc.service.schedule.utils.ScheduleTaskUtil;

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

    @Autowired
    private ScheduleService scheduleService;

    public DlmLimiterConfigEntity createAndBindToOrder(Long orderId, RateLimitConfiguration config) {
        checkLimiterConfig(config);
        DlmLimiterConfigEntity entity = mapper.modelToEntity(config);
        entity.setOrderId(orderId);
        return limiterConfigRepository.save(entity);
    }

    public RateLimitConfiguration getByOrderIdOrElseDefaultConfig(Long orderId) {
        Optional<DlmLimiterConfigEntity> entityOptional = limiterConfigRepository.findByOrderId(orderId);
        if (entityOptional.isPresent()) {
            return mapper.entityToModel(entityOptional.get());
        } else {
            return getDefaultLimiterConfig();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public RateLimitConfiguration updateByOrderId(Long orderId, RateLimitConfiguration rateLimit) {
        checkLimiterConfig(rateLimit);
        Optional<DlmLimiterConfigEntity> entityOptional = limiterConfigRepository.findByOrderId(orderId);
        if (entityOptional.isPresent()) {
            DlmLimiterConfigEntity entity = entityOptional.get();
            entity.setRowLimit(
                    rateLimit.getRowLimit() == null ? entity.getRowLimit() : rateLimit.getRowLimit());
            entity.setBatchSize(
                    rateLimit.getBatchSize() == null ? entity.getBatchSize() : rateLimit.getBatchSize());
            entity.setDataSizeLimit(rateLimit.getDataSizeLimit() == null ? entity.getDataSizeLimit()
                    : rateLimit.getDataSizeLimit());
            DataArchiveParameters parameters =
                    ScheduleTaskUtil.getDataArchiveParameters(scheduleService.nullSafeGetById(orderId));
            parameters.setRateLimit(mapper.entityToModel(entity));
            scheduleService.updateJobParametersById(orderId, JsonUtils.toJson(parameters));
            return mapper.entityToModel(limiterConfigRepository.save(entity));
        } else {
            throw new NotFoundException(ResourceType.ODC_DLM_LIMITER_CONFIG, "Id", orderId);
        }
    }

    public RateLimitConfiguration getDefaultLimiterConfig() {
        RateLimitConfiguration rateLimitConfiguration = new RateLimitConfiguration();
        rateLimitConfiguration.setRowLimit(defaultRowLimit);
        rateLimitConfiguration.setDataSizeLimit(defaultDataSizeLimit);
        rateLimitConfiguration.setBatchSize(defaultBatchSize);
        return rateLimitConfiguration;
    }

    private void checkLimiterConfig(RateLimitConfiguration limiterConfig) {
        if (limiterConfig.getRowLimit() != null && limiterConfig.getRowLimit() > maxRowLimit) {
            throw new IllegalArgumentException(String.format("The maximum row limit is %s rows/s.", maxRowLimit));
        }
        if (limiterConfig.getDataSizeLimit() != null && limiterConfig.getDataSizeLimit() > maxDataSizeLimit) {
            throw new IllegalArgumentException(String.format("The maximum data size is %s KB/s.", maxDataSizeLimit));
        }
    }
}
