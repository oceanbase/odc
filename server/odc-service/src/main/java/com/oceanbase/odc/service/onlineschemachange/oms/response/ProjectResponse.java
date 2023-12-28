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
package com.oceanbase.odc.service.onlineschemachange.oms.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-03
 * @since 4.2.0
 */
@Data
@JsonFormat(with = Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public class ProjectResponse {
    /**
     * 项目 ID
     */
    private String id;

    /**
     * 传输实例的 ID
     */
    private String workerGradeId;

    /**
     * 项目状态
     */
    private String status;

    /**
     * 项目类型
     */
    private String type;
}
