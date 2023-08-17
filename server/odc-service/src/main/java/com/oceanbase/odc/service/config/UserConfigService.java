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

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.core.shared.exception.BadRequestException;
import com.oceanbase.odc.core.shared.exception.InternalServerError;
import com.oceanbase.odc.core.shared.exception.NotFoundException;
import com.oceanbase.odc.metadb.config.UserConfigDAO;
import com.oceanbase.odc.metadb.config.UserConfigDO;

import lombok.extern.slf4j.Slf4j;

/**
 * service object for personal configuration
 *
 * @author yh263208
 * @date 2021-05-17 20:33
 * @since ODC_release_2.4.2
 */
@Service
@Slf4j
@Validated
@SkipAuthorize("odc internal usage")
public class UserConfigService {
    /**
     * data access object for user configuration
     */
    @Autowired
    private UserConfigDAO userConfigDAO;

    /**
     * insert a personal config
     *
     * @param config config object
     * @return config object
     * @throws IllegalArgumentException exception will be thrown when data is illegal
     * @throws InternalServerError error will be thrown when effect row is not equal to one
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    public UserConfigDO insert(@Valid @NotNull UserConfigDO config)
            throws IllegalArgumentException, InternalServerError {
        String configKey = config.getKey();
        int effectRow = userConfigDAO.insert(config);
        if (effectRow != 1) {
            log.error("Fail to insert a personal config setting, key={},value={},effectRow={}", configKey,
                    config.getValue(), effectRow);
            throw new InternalServerError("EffectRow is illegal");
        }
        log.info("Insert a personal config item successfully, userConfig={}", config);
        return config;
    }

    /**
     * query a list of personal config for a user
     *
     * @param userId user id
     * @return config map
     * @throws IllegalArgumentException exception will be thrown when data is illegal
     * @throws InternalServerError error will be thrown when config list's size is illegal
     */
    @NotNull(message = "Return value for OdcUserConfigService#query can not be null")
    public List<UserConfigDO> query(@NotNull Long userId) throws IllegalArgumentException, InternalServerError {
        return userConfigDAO.listByUserId(userId);
    }

    /**
     * query a personal config item
     *
     * @param userId user id
     * @param key config key
     * @return config value
     * @throws IllegalArgumentException exception will be thrown when data is illegal
     * @throws InternalServerError error will be thrown when config key or value is illegal
     */
    public String query(@NotNull Long userId, @NotBlank String key)
            throws IllegalArgumentException, InternalServerError {
        UserConfigDO config = userConfigDAO.get(userId, key);
        if (config == null) {
            log.warn("Fail to query a personal config, config is null, userId={},key={}", userId, key);
            return null;
        }
        if (config.getValue() == null) {
            log.error("Fail to query a personal config, value is null, key={},value={}", config.getKey(),
                    config.getValue());
            throw new InternalServerError("Key or value is null");
        }
        return config.getValue();
    }

    /**
     * update a personal config item
     *
     * @param config config object
     * @return config object
     * @throws IllegalArgumentException exception will be thrown when data is illegal
     * @throws InternalServerError error will be thrown when effect row is not equal to one
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    public UserConfigDO update(@Valid @NotNull UserConfigDO config)
            throws IllegalArgumentException, InternalServerError {
        UserConfigDO configDO = userConfigDAO.get(config.getUserId(), config.getKey());
        if (configDO == null) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"UserConfig", "Key", config.getKey()},
                    "UserConfig does not exist");
        }
        if (configDO.getValue().equals(config.getValue())) {
            throw new BadRequestException(ErrorCodes.BadRequest, new Object[] {},
                    "There are not any differences between userConfig in metaDB and userConfig input");
        }
        int effectRow = userConfigDAO.update(config);
        if (effectRow != 1) {
            log.error("Fail to update a personal config setting, key={},value={},effectRow={}", config.getKey(),
                    config.getValue(), effectRow);
            throw new InternalServerError("EffectRow is illegal");
        }
        log.info("Update a personal config item successfully, userConfig={}", config);
        return config;
    }

    /**
     * delete a personal config item
     *
     * @param config config object
     * @return config object
     * @throws IllegalArgumentException exception will be thrown when data is illegal
     * @throws InternalServerError error will be thrown when effect row is not equal to one
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    public UserConfigDO delete(@Valid @NotNull UserConfigDO config)
            throws IllegalArgumentException, InternalServerError {
        UserConfigDO configDO = userConfigDAO.get(config.getUserId(), config.getKey());
        if (configDO == null) {
            throw new NotFoundException(ErrorCodes.NotFound, new Object[] {"UserConfig", "Key", config.getKey()},
                    "UserConfig does not exist");
        }
        int effectRow = userConfigDAO.delete(config.getUserId(), config.getKey());
        if (effectRow != 1) {
            log.error("Fail to delete a personal config setting, key={},effectRow={}", config.getKey(),
                    effectRow);
            throw new InternalServerError("EffectRow is illegal");
        }
        log.info("Delete a personal config item successfully, userConfig={}", config);
        return config;
    }
}
