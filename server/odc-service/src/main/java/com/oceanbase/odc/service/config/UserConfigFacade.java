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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.core.shared.exception.UnexpectedException;
import com.oceanbase.odc.service.config.model.UserConfig;

/**
 * Facade layer for user config setting
 *
 * @author yh263208
 * @date 2021-05-18 15:38
 * @since ODC_release_2.4.2
 */
@Validated
public interface UserConfigFacade {
    /**
     * Operator for OdcUserConfig
     *
     * @author yh263208
     * @date 2021-05-28 17:57
     * @since ODC_release_2.4.2 user config service object
     */
    interface UserConfigOperator {
        void perform(UserConfig userConfig) throws Throwable;
    }

    /**
     * Query User config from thread local
     *
     * @return config object
     */
    @Valid
    @NotNull
    UserConfig currentUserConfig();

    /**
     * Query User config from Cache
     *
     * @param userId user id
     * @return config object
     */
    @Valid
    @NotNull
    UserConfig queryByCache(@NotNull Long userId);

    /**
     * get all personal config
     *
     * @param userId user id
     * @return config map
     */
    @Valid
    @NotNull
    UserConfig query(@NotNull Long userId) throws UnexpectedException;

    /**
     * set a user config item
     *
     * @param userId user id
     * @param userConfig config object
     */
    @Transactional(rollbackFor = Throwable.class, propagation = Propagation.REQUIRED)
    UserConfig put(@NotNull Long userId, @Valid @NotNull UserConfig userConfig) throws UnexpectedException;

    /**
     * Apply user config
     *
     * @param operator operator for user config
     * @return config which performed
     */
    UserConfig apply(@NotNull UserConfigOperator operator);
}
