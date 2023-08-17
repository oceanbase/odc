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
package com.oceanbase.odc.service.config;

import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.config.OrganizationConfigDAO;
import com.oceanbase.odc.metadb.config.OrganizationConfigEntity;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.config.util.OrganizationConfigUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2021/7/16 下午5:04
 * @Description: []
 */
@Service
@Slf4j
@Validated
@Authenticated
public class OrganizationConfigService {

    @Autowired
    private OrganizationConfigDAO organizationConfigDAO;

    @Autowired
    private SystemConfigService systemConfigService;

    @NotNull(message = "query result should not be null")
    @PreAuthenticate(actions = "read", resourceType = "ODC_SYSTEM_CONFIG", isForAll = true)
    public List<Configuration> query(@NotNull Long organizationId) {
        return internalQuery(organizationId);
    }

    /**
     * bypass authenticate while query user config, for default value
     */
    @NotNull(message = "query result should not be null")
    @SkipAuthorize("odc internal usage")
    public List<Configuration> internalQuery(@NotNull Long organizationId) {
        List<OrganizationConfigEntity> configsInDb = organizationConfigDAO.listByOrganizationId(organizationId);
        List<Configuration> configurationsInDb = OrganizationConfigUtil.convertDO2DTO(configsInDb);
        List<Configuration> defaultConfigurations =
                systemConfigService.queryByKeyPrefixes(Arrays.asList("sqlexecute", "connect"));
        return OrganizationConfigUtil.mergeConfigurations(defaultConfigurations, configurationsInDb);
    }

    @PreAuthenticate(actions = "read", resourceType = "ODC_SYSTEM_CONFIG", isForAll = true)
    public String query(@NotNull Long organizationId, @NotNull String key) {
        OrganizationConfigEntity configEntity = organizationConfigDAO.getByIdAndKey(organizationId, key);
        if (configEntity == null) {
            log.warn("Fail to query a organization config, config is null, organizationId={},key={}", organizationId,
                    key);
            return null;
        }
        if (configEntity.getValue() == null) {
            log.error("Fail to query a organization config, value is null, key={},value={}", configEntity.getKey(),
                    configEntity.getValue());
            throw new InternalServerError("Key or value is null");
        }
        return configEntity.getValue();
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    @PreAuthenticate(actions = "update", resourceType = "ODC_SYSTEM_CONFIG", isForAll = true)
    public Configuration update(@Valid @NotNull Configuration config, @NotNull Long currentOrganizationId,
            @NotNull Long currentUserId)
            throws IllegalArgumentException, InternalServerError {
        OrganizationConfigEntity configEntity = OrganizationConfigUtil.convertDTO2DO(config);
        configEntity.setOrganizationId(currentOrganizationId);
        configEntity.setLastModifierId(currentUserId);
        OrganizationConfigEntity configEntityInDb =
                organizationConfigDAO.getByIdAndKey(currentOrganizationId, config.getKey());
        if (configEntity == null) {
            throw new NotFoundException(ErrorCodes.NotFound,
                    new Object[] {"OrganizationConfig", "Key", config.getKey()},
                    "OrganizationConfig does not exist");
        }
        if (configEntityInDb.getValue().equals(config.getValue())) {
            throw new BadRequestException(ErrorCodes.BadRequest, new Object[] {},
                    "There are not any differences between organizationConfig in metadb and organizationConfig input");
        }
        int effectRow = organizationConfigDAO.update(configEntity);
        if (effectRow != 1) {
            log.error("Fail to update an organization config setting, key={},value={},effectRow={}", config.getKey(),
                    config.getValue(), effectRow);
            throw new InternalServerError("EffectRow is illegal");
        }
        log.info("Update a organization config item successfully, organizationConfig={}", config);
        return config;
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    @PreAuthenticate(actions = "create", resourceType = "ODC_SYSTEM_CONFIG", isForAll = true)
    public Configuration insert(@Valid @NotNull Configuration config, @NotNull Long currentOrganizationId,
            @NotNull Long currentUserId)
            throws IllegalArgumentException, InternalServerError {
        OrganizationConfigEntity configEntity = OrganizationConfigUtil.convertDTO2DO(config);
        configEntity.setOrganizationId(currentOrganizationId);
        configEntity.setCreatorId(currentUserId);
        configEntity.setLastModifierId(currentUserId);
        String configKey = config.getKey();
        int effectRow = organizationConfigDAO.insert(configEntity);
        if (effectRow != 1) {
            log.error("Fail to insert an organization config setting, key={}, value={}, effectRow={}", configKey,
                    config.getValue(), effectRow);
            throw new InternalServerError("EffectRow is illegal");
        }
        log.info("Insert an organization config item successfully, organizationConfig={}", config);
        return config;
    }

    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    @PreAuthenticate(actions = "update", resourceType = "ODC_SYSTEM_CONFIG", isForAll = true)
    public List<Configuration> batchUpdate(@NotNull List<Configuration> updateConfigurations,
            @NotNull Long currentOrganizationId, @NotNull Long currentUserId) {
        OrganizationConfigUtil.validateOrganizationConfig(updateConfigurations);
        List<Configuration> currentConfiguration = query(currentOrganizationId);
        List<Configuration> mergedConfiguration =
                OrganizationConfigUtil.mergeConfigurations(currentConfiguration, updateConfigurations);
        for (Configuration config : mergedConfiguration) {
            String configValueInDb = query(currentOrganizationId, config.getKey());
            if (configValueInDb != null) {
                if (!configValueInDb.equals(config.getValue())) {
                    update(config, currentOrganizationId, currentUserId);
                }
            } else {
                insert(config, currentOrganizationId, currentUserId);
            }
        }
        return updateConfigurations;
    }
}
