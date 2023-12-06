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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.common.model.Stats;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.connection.ConnectionBatchImportPreviewer;
import com.oceanbase.odc.service.connection.ConnectionHelper;
import com.oceanbase.odc.service.connection.ConnectionService;
import com.oceanbase.odc.service.connection.ConnectionStatusManager.CheckState;
import com.oceanbase.odc.service.connection.database.DatabaseService;
import com.oceanbase.odc.service.connection.database.model.Database;
import com.oceanbase.odc.service.connection.database.model.DatabaseUser;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.ConnectionPreviewBatchImportResp;
import com.oceanbase.odc.service.connection.model.GenerateConnectionStringReq;
import com.oceanbase.odc.service.connection.model.QueryConnectionParams;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/4/12 20:27
 * @Description: []
 */
@RestController
@RequestMapping("/api/v2/datasource")
public class DataSourceController {
    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private ConnectionHelper connectionHelper;

    @Autowired
    private ConnectionBatchImportPreviewer connectionBatchImportPreviewer;

    @Value("${odc.integration.bastion.enabled:false}")
    private boolean bastionEnabled;

    @ApiOperation(value = "createDataSource", notes = "Create a datasource")
    @RequestMapping(value = "/datasources", method = RequestMethod.POST)
    public SuccessResponse<ConnectionConfig> createDataSource(@RequestBody ConnectionConfig config) {
        if (bastionEnabled) {
            return Responses.success(connectionService.create(config, OdcConstants.DEFAULT_ADMIN_USER_ID, true));
        }
        return Responses.success(connectionService.create(config));
    }

    @ApiOperation(value = "deleteDataSource", notes = "Delete a datasource")
    @RequestMapping(value = "/datasources/{id:[\\d]+}", method = RequestMethod.DELETE)
    public SuccessResponse<ConnectionConfig> deleteDataSource(@PathVariable Long id) {
        return Responses.success(connectionService.delete(id));
    }

    @ApiOperation(value = "deleteDataSources", notes = "Batch delete datasources")
    @RequestMapping(value = "/datasources/batchDelete", method = RequestMethod.DELETE)
    public ListResponse<ConnectionConfig> batchDelete(@RequestBody Set<Long> ids) {
        return Responses.list(connectionService.delete(ids));
    }


    @ApiOperation(value = "updateDataSource", notes = "Update a datasource")
    @RequestMapping(value = "/datasources/{id:[\\d]+}", method = RequestMethod.PUT)
    public SuccessResponse<ConnectionConfig> updateDataSource(@PathVariable Long id,
            @RequestBody ConnectionConfig connectionConfig) throws InterruptedException {
        return Responses.success(connectionService.update(id, connectionConfig));
    }

    @ApiOperation(value = "getDataSource", notes = "Detail a datasource")
    @RequestMapping(value = "/datasources/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<ConnectionConfig> getDataSource(@PathVariable Long id) {
        return Responses.success(connectionService.detail(id));
    }


    @ApiOperation(value = "listDataSources", notes = "List all datasources")
    @RequestMapping(value = "/datasources", method = RequestMethod.GET)
    public PaginatedResponse<ConnectionConfig> listDataSources(
            @RequestParam(required = false, defaultValue = "true", name = "basic") Boolean basic,
            @RequestParam(required = false, name = "projectId") Long projectId,
            @RequestParam(required = false, name = "userId") Long userId,
            @RequestParam(required = false, defaultValue = "read", name = "minPrivilege") String minPrivilege,
            @RequestParam(required = false, name = "type") List<ConnectType> types,
            @RequestParam(required = false, name = "dialectType") List<DialectType> dialectTypes,
            @RequestParam(required = false, name = "enabled") List<Boolean> enabledList,
            @RequestParam(required = false, name = "fuzzySearchKeyword") String fuzzySearchKeyword,
            @RequestParam(required = false, name = "id") Set<Long> ids,
            @RequestParam(required = false, name = "clusterName") List<String> clusterNames,
            @RequestParam(required = false, name = "tenantName") List<String> tenantNames,
            @RequestParam(required = false, name = "permittedAction") List<String> permittedActions,
            @RequestParam(required = false, name = "hostPort") String hostPort,
            @RequestParam(required = false, name = "name") String name,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        if (Objects.nonNull(projectId)) {
            return Responses.paginated(connectionService.listByProjectId(projectId, basic));
        }
        Boolean enabled = CollectionUtils.size(enabledList) == 1 ? enabledList.get(0) : null;
        QueryConnectionParams params = QueryConnectionParams.builder()
                .types(types)
                .dialectTypes(dialectTypes)
                .enabled(enabled)
                .fuzzySearchKeyword(fuzzySearchKeyword)
                .minPrivilege(minPrivilege)
                .clusterNames(clusterNames)
                .tenantNames(tenantNames)
                .permittedActions(permittedActions)
                .relatedUserId(userId)
                .hostPort(hostPort)
                .name(name)
                .ids(ids)
                .build();
        return Responses.paginated(connectionService.list(params, pageable));
    }

    @ApiOperation(value = "listDatabases", notes = "List databases by DataSourceId")
    @RequestMapping(value = "/datasources/{id:[\\d]+}/databases", method = RequestMethod.GET)
    public PaginatedResponse<Database> listDatabases(@PathVariable Long id,
            @RequestParam(required = false, name = "name") String name,
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable) {
        return Responses.paginated(databaseService.listDatabasesByDataSource(id, name, pageable));
    }


    @ApiOperation(value = "generateConnectionStr", notes = "generate connection string")
    @RequestMapping(value = "/help/generateConnectionStr", method = RequestMethod.POST)
    public SuccessResponse<String> generateConnectionStr(@RequestBody GenerateConnectionStringReq req) {
        // TODO：公有云支持生成连接串, 这里交互比较特殊在于 host/port 的值是没有的，需要给出公网地址，需要和 OCP 进行交互
        return Responses.single(connectionHelper.generateConnectionStr(req));
    }

    @ApiOperation(value = "previewBatchImportDataSources", notes = "Parse imported file")
    @RequestMapping(value = "/datasources/previewBatchImport", method = RequestMethod.POST)
    public SuccessResponse<ConnectionPreviewBatchImportResp> previewBatchImportDataSources(
            @RequestParam MultipartFile file) throws IOException {
        return Responses.success(connectionBatchImportPreviewer.preview(file));
    }

    @ApiOperation(value = "batchCreateDataSources", notes = "Batch create datasources")
    @RequestMapping(value = "/datasources/batchCreate", method = RequestMethod.POST)
    public SuccessResponse<List<ConnectionConfig>> batchCreateDataSources(
            @RequestBody List<ConnectionConfig> connectionConfigs) {
        return Responses.success(connectionService.batchCreate(connectionConfigs));
    }

    @ApiOperation(value = "statusDataSources", notes = "Obtain datasource status")
    @RequestMapping(value = "/datasources/status", method = RequestMethod.POST)
    public SuccessResponse<Map<Long, CheckState>> status(@RequestBody Set<Long> ids) {
        return Responses.success(connectionService.getStatus(ids));
    }

    @ApiOperation(value = "statsDataSource", notes = "datasource stats information")
    @RequestMapping(value = "/datasources/stats", method = RequestMethod.GET)
    public SuccessResponse<Stats> stats() {
        return Responses.success(
                connectionService.list(QueryConnectionParams.builder().build(), Pageable.unpaged()).getStats());
    }

    @ApiOperation(value = "exists", notes = "Returns whether a datasource exists")
    @RequestMapping(value = "/datasources/exists", method = RequestMethod.GET)
    public SuccessResponse<Boolean> exists(@RequestParam String name) {
        return Responses.success(connectionService.exists(name));
    }

    @ApiOperation(value = "syncDataSource", notes = "syncDataSource")
    @RequestMapping(value = "/datasources/{id:[\\d]+}/sync", method = RequestMethod.POST)
    public SuccessResponse<Boolean> syncDataSource(@PathVariable Long id) throws InterruptedException {
        return Responses.success(databaseService.syncDataSourceSchemas(id));
    }

    @ApiOperation(value = "listUsers", notes = "list users in datasource")
    @RequestMapping(value = "/datasources/{id:[\\d]+}/users", method = RequestMethod.GET)
    public PaginatedResponse<DatabaseUser> listUsers(@PathVariable Long id) {
        return Responses.paginated(databaseService.listUsers(id));
    }

}
