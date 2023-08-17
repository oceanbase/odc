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
package com.oceanbase.odc.metadb.config;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

/**
 * dao for user config
 *
 * @author yh263208
 * @date 2021-05-17 19:31
 * @since ODC_release_2.4.2
 */
@Component
public interface UserConfigDAO {
    /**
     * insert a user config
     *
     * @param userConfig user configuration
     * @return effect row
     */
    int insert(UserConfigDO userConfig);

    /**
     * select a user config item
     *
     * @param userId user id
     * @param key config key
     * @return configuration object
     */
    UserConfigDO get(@Param("userId") Long userId, @Param("key") String key);

    /**
     * select a user config list
     *
     * @param userId user id
     * @return list of a configuration
     */
    List<UserConfigDO> listByUserId(@Param("userId") Long userId);

    /**
     * update a user configuration
     *
     * @param config config value
     * @return effect row
     */
    int update(UserConfigDO config);

    /**
     * delete a user configuration
     *
     * @param userId user id
     * @param key configuration key
     * @return effect row
     */
    int delete(@Param("userId") Long userId, @Param("key") String key);
}
