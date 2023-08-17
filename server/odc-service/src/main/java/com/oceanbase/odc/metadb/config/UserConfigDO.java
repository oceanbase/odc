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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * packaged object for dao (personal configuration)
 *
 * @author yh263208
 * @date 2021-05-17 18:16
 * @since ODC_release_2.4.2
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class UserConfigDO {
    /**
     * user id
     */
    @NotNull(message = "User id can not be null")
    private Long userId;
    /**
     * config key
     */
    @NotBlank(message = "Config key can not be blank")
    @NotNull(message = "Config key can not be null")
    private String key;
    /**
     * config value
     */
    private String value;
    /**
     * create time for this configuration
     */
    private Date createTime;
    /**
     * last update time for this configuration
     */
    private Date updateTime;
    /**
     * notice for this configuration
     */
    private String description;

    public UserConfigDO() {}

    public UserConfigDO(String key, Object value, String descrition) {
        Validate.notNull(key, "Key for OdcConfigDO can not be null");
        Validate.notNull(value, "Value for OdcConfigDO can not be null");
        this.key = key;
        this.value = value.toString();
        this.description = descrition;
    }
}
