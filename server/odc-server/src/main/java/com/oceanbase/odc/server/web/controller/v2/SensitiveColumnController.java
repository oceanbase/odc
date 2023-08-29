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

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.service.common.model.SetEnabledReq;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.datasecurity.SensitiveColumnService;
import com.oceanbase.odc.service.datasecurity.model.QuerySensitiveColumnParams;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumn;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningReq;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnScanningTaskInfo;
import com.oceanbase.odc.service.datasecurity.model.SensitiveColumnStats;
import com.oceanbase.odc.service.datasecurity.model.UpdateSensitiveColumnsReq;

import io.swagger.annotations.ApiOperation;

/**
 * @author gaoda.xy
 * @date 2023/5/9 16:25
 */
@RestController
@RequestMapping("api/v2/collaboration/projects/{projectId:[\\d]+}/sensitiveColumns")
public class SensitiveColumnController {

    @Autowired
    private SensitiveColumnService service;

    @ApiOperation(value = "sensitiveColumnExists", notes = "Check if sensitive column exists")
    @RequestMapping(value = "/exists", method = RequestMethod.POST)
    public SuccessResponse<Boolean> exists(@PathVariable Long projectId, @RequestBody SensitiveColumn column) {
        return Responses.success(service.exists(projectId, column));
    }

    @ApiOperation(value = "batchCreateSensitiveColumns", notes = "Batch create sensitive columns")
    @RequestMapping(value = "/batchCreate", method = RequestMethod.POST)
    public ListResponse<SensitiveColumn> batchCreateSensitiveColumns(@PathVariable Long projectId,
            @RequestBody List<SensitiveColumn> columns) {
        return Responses.list(service.batchCreate(projectId, columns));
    }

    @ApiOperation(value = "detailSensitiveColumn", notes = "View sensitive column details")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<SensitiveColumn> detailSensitiveColumn(@PathVariable Long projectId,
            @PathVariable Long id) {
        return Responses.success(service.detail(projectId, id));
    }

    @ApiOperation(value = "batchUpdateSensitiveColumns", notes = "Batch update masking algorithm of sensitive columns")
    @RequestMapping(value = "/batchUpdate", method = RequestMethod.PUT)
    public ListResponse<SensitiveColumn> batchUpdateSensitiveColumn(@PathVariable Long projectId,
            @RequestBody UpdateSensitiveColumnsReq req) {
        return Responses.list(service.batchUpdate(projectId, req.getSensitiveColumnIds(), req.getMaskingAlgorithmId()));
    }

    @ApiOperation(value = "batchDeleteSensitiveColumns", notes = "Batch delete sensitive columns")
    @RequestMapping(value = "/batchDelete", method = RequestMethod.DELETE)
    public ListResponse<SensitiveColumn> batchDeleteSensitiveColumns(@PathVariable Long projectId,
            @RequestBody List<Long> ids) {
        return Responses.list(service.batchDelete(projectId, ids));
    }

    @ApiOperation(value = "listSensitiveColumns", notes = "List sensitive columns")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public PaginatedResponse<SensitiveColumn> listSensitiveColumns(@PathVariable Long projectId,
            @RequestParam(name = "fuzzyTableColumn", required = false) String fuzzyTableColumn,
            @RequestParam(name = "datasource", required = false) List<Long> datasourceIds,
            @RequestParam(name = "database", required = false) List<Long> databaseIds,
            @RequestParam(name = "maskingAlgorithm", required = false) List<Long> maskingAlgorithmIds,
            @RequestParam(name = "enabled", required = false) List<Boolean> enabledList,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.ASC) Pageable pageable) {
        Boolean enabled = CollectionUtils.size(enabledList) == 1 ? enabledList.get(0) : null;
        QuerySensitiveColumnParams params = QuerySensitiveColumnParams.builder()
                .fuzzyTableColumn(fuzzyTableColumn)
                .datasourceIds(datasourceIds)
                .databaseIds(databaseIds)
                .maskingAlgorithmIds(maskingAlgorithmIds)
                .enabled(enabled).build();
        return Responses.paginated(service.list(projectId, params, pageable));
    }

    @ApiOperation(value = "statsSensitiveColumns", notes = "Get sensitive column stats")
    @RequestMapping(value = "/stats", method = RequestMethod.GET)
    public SuccessResponse<SensitiveColumnStats> getStats(@PathVariable Long projectId) {
        return Responses.success(service.stats(projectId));
    }

    @ApiOperation(value = "setEnabled", notes = "Enable or disable a sensitive column")
    @RequestMapping(value = "/{id:[\\d]+}/setEnabled", method = RequestMethod.POST)
    public SuccessResponse<SensitiveColumn> setEnabled(@PathVariable Long projectId,
            @PathVariable Long id, @RequestBody SetEnabledReq req) {
        return Responses.success(service.setEnabled(projectId, id, req.getEnabled()));
    }

    @ApiOperation(value = "startScanning", notes = "Start a sensitive column scanning task")
    @RequestMapping(value = "/startScanning", method = RequestMethod.POST)
    public SuccessResponse<SensitiveColumnScanningTaskInfo> startScanning(@PathVariable Long projectId,
            @RequestBody SensitiveColumnScanningReq req) {
        return Responses.success(service.startScanning(projectId, req));
    }

    @ApiOperation(value = "getScanningResults", notes = "Get sensitive column scanning results")
    @RequestMapping(value = "/getScanningResults", method = RequestMethod.GET)
    public SuccessResponse<SensitiveColumnScanningTaskInfo> getScanningResults(@PathVariable Long projectId,
            @RequestParam String taskId) {
        return Responses.success(service.getScanningResults(projectId, taskId));
    }

}
