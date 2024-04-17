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
package com.oceanbase.odc.service.task.service;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.oceanbase.odc.service.task.runtime.CreateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.task.runtime.QueryDatabaseChangeChangingOrderResp;

@Service
@Validated
public class DatabaseChangeChangingOrderTemplateService {

    @Transactional
    public Boolean createOrModifyDatabaseTemplate(
            @NotNull @Valid CreateDatabaseChangeChangingOrderReq req) {
        throw new RuntimeException("本次pr暂不实现，交由后续pr提交");
    }

    public QueryDatabaseChangeChangingOrderResp queryDatabaseTemplateById(@NotNull @Min(value = 0) Long id) {
        throw new RuntimeException("本次pr暂不实现，交由后续pr提交");
    }


    public Page<QueryDatabaseChangeChangingOrderResp> listDatabaseTemplate(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        throw new RuntimeException("本次pr暂不实现，交由后续pr提交");
    }

    @Transactional
    public Boolean deleteDatabseTemplateById(@NotNull @Min(value = 0) Long id) {
        throw new RuntimeException("本次pr暂不实现，交由后续pr提交");
    }

}
