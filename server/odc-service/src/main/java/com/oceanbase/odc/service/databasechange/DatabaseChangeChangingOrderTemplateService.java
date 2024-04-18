/*
 * Copyright (c) 2024 OceanBase.
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
package com.oceanbase.odc.service.databasechange;

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

import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.databasechange.model.CreateDatabaseChangeChangingOrderReq;
import com.oceanbase.odc.service.databasechange.model.QueryDatabaseChangeChangingOrderResp;

@Service
@Validated
public class DatabaseChangeChangingOrderTemplateService {

    @Transactional
    public Boolean createDatabaseChangingOrderTemplate(
            @NotNull @Valid CreateDatabaseChangeChangingOrderReq req) {
        throw new NotImplementedException("Unsupported now");
    }

    @Transactional
    public Boolean modifyDatabaseChangingOrderTemplate(
        @NotNull @Valid CreateDatabaseChangeChangingOrderReq req) {
        throw new NotImplementedException("Unsupported now");
    }

    public QueryDatabaseChangeChangingOrderResp queryDatabaseChangingOrderTemplateById(@NotNull @Min(value = 0) Long id) {
        throw new NotImplementedException("Unsupported now");
    }


    public Page<QueryDatabaseChangeChangingOrderResp> listDatabaseChangingOrderTemplates(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        throw new NotImplementedException("Unsupported now");
    }

    @Transactional
    public Boolean deleteDatabaseChangingOrderTemplateById(@NotNull @Min(value = 0) Long id) {
        throw new NotImplementedException("Unsupported now");
    }

}
