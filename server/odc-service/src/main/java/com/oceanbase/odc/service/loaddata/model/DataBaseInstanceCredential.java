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
package com.oceanbase.odc.service.loaddata.model;

import java.io.Serializable;

import com.oceanbase.odc.service.cloud.model.CloudProvider;

import lombok.Data;
import lombok.ToString;

/**
 * This is used for OceanBase instance in OBCloud.
 *
 * @author xien.sxe
 * @date 2024/3/4
 * @since 1.0.0
 */
@Data
@ToString(exclude = "password")
public class DataBaseInstanceCredential implements Serializable {

    private static final long serialVersionUID = -6045851446132148119L;

    private String username;

    private String password;

    private String region;

    private CloudProvider provider;
}
