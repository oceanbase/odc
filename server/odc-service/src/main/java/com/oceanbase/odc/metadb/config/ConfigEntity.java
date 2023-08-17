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

import java.util.Date;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.Validate;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2021/7/28 下午1:03
 * @Description: []
 */
@Data
public class ConfigEntity {
    /**
     * Unique id for each line of configuration
     */
    @NotNull
    private long id;

    /**
     * Label of this configuration
     */
    private String label;

    /**
     * configuration key
     */
    @NotBlank
    private String key;

    /**
     * configuration value
     */
    private String value;

    /**
     * Created time
     */
    private Date createTime;

    /**
     * Latest update time
     */
    private Date updateTime;

    /**
     * description of this configuration
     */
    private String description;

    /**
     * Configs creator's user ID
     */
    private long creatorId;

    /**
     * Last modifier's user ID
     */
    private long lastModifierId;

    public ConfigEntity() {}

    public ConfigEntity(String key, Object value, String description) {
        Validate.notNull(key, "Key for ConfigEntity can not be null");
        Validate.notNull(value, "Value for ConfigEntity can not be null");
        this.key = key;
        this.value = value.toString();
        this.description = description;
    }
}
