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
package com.oceanbase.odc.service.shadowtable.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2022/9/22 下午2:25
 * @Description: []
 */
@Data
public class SetSkippedReq {
    /**
     * 是否跳过
     */
    @NotNull
    private Boolean setSkip;

    @Valid
    @NotEmpty(message = "table comparing ids cannot be null or empty")
    private List<Long> tableComparingIds;

}
