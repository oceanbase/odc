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
package com.oceanbase.odc.server.web.controller.v1;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.dml.TableDataService;
import com.oceanbase.odc.service.dml.model.BatchDataModifyReq;
import com.oceanbase.odc.service.dml.model.BatchDataModifyResp;
import com.oceanbase.odc.service.session.ConnectSessionService;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@Validated
@RestController
@RequestMapping("/api/v1/data")
public class DBTableDataController {

    @Autowired
    private TableDataService tableDataService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "batchGetModifySql", notes = "批量获取修改数据的sql，包含 INSERT/UPDATE/DELETE")
    @RequestMapping(value = "/batchGetModifySql/{sid:.*}", method = RequestMethod.POST)
    public OdcResult<BatchDataModifyResp> batchGetModifySql(@PathVariable String sid,
            @RequestBody @NotNull @Valid BatchDataModifyReq req) {
        return OdcResult.ok(tableDataService.batchGetModifySql(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid)), req));
    }

}
