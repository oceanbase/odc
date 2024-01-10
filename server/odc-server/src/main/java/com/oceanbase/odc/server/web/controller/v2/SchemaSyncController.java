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
package com.oceanbase.odc.server.web.controller.v2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.flow.task.model.DBObjectStructureComparisonResp;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp;
import com.oceanbase.odc.service.flow.task.model.DBStructureComparisonResp.OperationType;
import com.oceanbase.odc.service.shadowtable.ShadowTableComparingService;
import com.oceanbase.odc.service.shadowtable.model.SetSkippedReq;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncReq;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncResp;
import com.oceanbase.odc.service.shadowtable.model.ShadowTableSyncResp.TableComparing;

/**
 * @Author: Lebie
 * @Date: 2022/9/19 下午2:54
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/schema-sync")
public class SchemaSyncController {
    @Autowired
    private ShadowTableComparingService shadowTableComparingService;

    @RequestMapping(value = "/shadowTableSyncs", method = RequestMethod.POST)
    public SuccessResponse<String> createShadowTableSync(@RequestBody ShadowTableSyncReq shadowTableSyncReq) {
        return Responses.success(shadowTableComparingService.createShadowTableSync(shadowTableSyncReq));
    }

    @RequestMapping(value = "/shadowTableSyncs/{id}", method = RequestMethod.GET)
    public SuccessResponse<ShadowTableSyncResp> listShadowTableSyncs(@PathVariable Long id) {
        return Responses.success(shadowTableComparingService.listShadowTableSyncs(id));
    }

    @RequestMapping(value = "/shadowTableSyncs/{id}/tables/{tableComparingId}", method = RequestMethod.GET)
    public SuccessResponse<TableComparing> getTableComparing(@PathVariable Long id,
            @PathVariable Long tableComparingId) {
        return Responses.success(shadowTableComparingService.getTableComparing(id, tableComparingId));
    }

    @RequestMapping(value = "/shadowTableSyncs/{id}/tables/batchSetSkipped", method = RequestMethod.POST)
    public ListResponse<TableComparing> skipTableComparing(@PathVariable Long id,
            @RequestBody SetSkippedReq setSkippedReq) {
        return Responses.list(shadowTableComparingService.setSkipTableComparing(id, setSkippedReq));
    }

    @RequestMapping(value = "/structureComparison/{id}", method = RequestMethod.GET)
    public SuccessResponse<DBStructureComparisonResp> listStructureComparisonResult(@PathVariable Long id,
            @RequestParam OperationType operationType) {
        throw new UnsupportedOperationException("structure comparison not supported yet");
    }

    @RequestMapping(value = "/structureComparison/{id}/{structureComparisonId}", method = RequestMethod.GET)
    public SuccessResponse<DBObjectStructureComparisonResp> getStructureComparisonResult(@PathVariable Long id,
            @PathVariable Long structureComparisonId) {
        throw new UnsupportedOperationException("structure comparison not supported yet");
    }
}
