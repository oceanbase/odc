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

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author: Lebie
 * @Date: 2021/7/27 下午8:32
 * @Description: []
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemConfigEntity extends ConfigEntity {

    /**
     * application name
     */
    private String application;

    /**
     * profile for Spring Cloud Config
     */
    private String profile;

}
