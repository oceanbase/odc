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
package com.oceanbase.odc.service.config.model;

import javax.validation.constraints.NotBlank;

import com.oceanbase.odc.metadb.config.UserConfigEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for all configurations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {
    /**
     * Config key
     */
    @NotBlank(message = "Config key can not be null or blank")
    private String key;

    /**
     * Config value
     */
    private String value;

    public static Configuration of(UserConfigEntity entity) {
        return new Configuration(entity.getKey(), entity.getValue());
    }

    public UserConfigEntity toEntity(Long userId) {
        UserConfigEntity entity = new UserConfigEntity();
        entity.setUserId(userId);
        entity.setKey(this.key);
        entity.setValue(this.value);
        return entity;
    }

}
