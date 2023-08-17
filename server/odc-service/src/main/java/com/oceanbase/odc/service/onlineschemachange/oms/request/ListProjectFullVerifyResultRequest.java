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
package com.oceanbase.odc.service.onlineschemachange.oms.request;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Range;

import lombok.Data;

/**
 * @author yaobin
 * @date 2023-06-01
 * @since 4.2.0
 */
@Data
public class ListProjectFullVerifyResultRequest extends BaseOmsRequest {
    /**
     * 项目ID
     */
    @NotBlank(message = "project id can not be blank")
    private String projectId;

    /**
     * 源端库名
     */
    @Size(min = 1, message = "source schemas can not be null")
    private String[] sourceSchemas;
    /**
     * 目标端库名
     */
    @Size(min = 1, message = "dest schemas can not be null")
    private String[] destSchemas;
    /**
     * 状态
     */
    private String[] status;
    /**
     * 当前页
     */
    @Min(1)
    private Integer pageNumber = 1;
    /**
     * 每页个数
     */
    @Range(min = 1, max = 150)
    private Integer pageSize = 10;
}
