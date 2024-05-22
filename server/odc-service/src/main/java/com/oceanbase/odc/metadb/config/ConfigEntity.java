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

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotBlank;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2021/7/28 下午1:03
 * @Description: []
 */
@Data
@MappedSuperclass
public class ConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * configuration key
     */
    @NotBlank
    @Column(name = "`key`")
    private String key;

    /**
     * configuration value
     */
    @Column(name = "`value`")
    private String value;

    /**
     * Created time
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * Latest update time
     */
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * description of this configuration
     */
    private String description;

}
