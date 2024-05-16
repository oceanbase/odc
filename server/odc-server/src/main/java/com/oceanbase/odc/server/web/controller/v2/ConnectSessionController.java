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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.exception.NotImplementedException;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.connection.model.CreateSessionResp;
import com.oceanbase.odc.service.connection.model.DBSessionResp;
import com.oceanbase.odc.service.connection.model.MultiSessionsReq;
import com.oceanbase.odc.service.db.session.DBSessionService;
import com.oceanbase.odc.service.db.session.KillSessionOrQueryReq;
import com.oceanbase.odc.service.db.session.KillSessionResult;
import com.oceanbase.odc.service.dml.ValueEncodeType;
import com.oceanbase.odc.service.partitionplan.PartitionPlanService;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanPreViewResp;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanPreviewReq;
import com.oceanbase.odc.service.session.ConnectConsoleService;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.session.model.AsyncExecuteResultResp;
import com.oceanbase.odc.service.session.model.BinaryContent;
import com.oceanbase.odc.service.session.model.QueryTableOrViewDataReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteReq;
import com.oceanbase.odc.service.session.model.SqlAsyncExecuteResp;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.sqlcheck.SqlCheckService;
import com.oceanbase.odc.service.sqlcheck.model.CheckResult;
import com.oceanbase.odc.service.sqlcheck.model.MultipleSqlCheckReq;
import com.oceanbase.odc.service.sqlcheck.model.SqlCheckReq;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

import io.swagger.annotations.ApiOperation;

/**
 * @Author: Lebie
 * @Date: 2023/6/5 10:19
 * @Description: []
 */
@Validated
@RestController
@RequestMapping("/api/v2/datasource")
public class ConnectSessionController {

    @Autowired
    private ConnectSessionService sessionService;
    @Autowired
    private ConnectConsoleService consoleService;
    @Autowired
    private SqlCheckService sqlCheckService;
    @Autowired
    private DBSessionService dbSessionService;
    @Autowired
    private PartitionPlanService partitionPlanService;

    @ApiOperation(value = "createSessionByDataSource", notes = "create connect session by a DataSource")
    @RequestMapping(value = "/datasources/{dataSourceId:[\\d]+}/sessions", method = RequestMethod.POST)
    public SuccessResponse<CreateSessionResp> createSessionByDataSource(@PathVariable Long dataSourceId) {
        return Responses.success(sessionService.createByDataSourceId(dataSourceId));
    }

    @ApiOperation(value = "createSessionByDatabase", notes = "create connect session by a Database")
    @RequestMapping(value = "/databases/{databaseId:[\\d]+}/sessions", method = RequestMethod.POST)
    public SuccessResponse<CreateSessionResp> createSessionByDatabase(@PathVariable Long databaseId) {
        return Responses.success(sessionService.createByDatabaseId(databaseId));
    }

    /**
     * 异步执行sql <br>
     * asyncExecute 和 asyncExecuteQueryData 功能完全一样，区分是为了根据不同场景配置拦截策略 <br>
     * - asyncExecute 为 SQL 窗口场景 <br>
     * - asyncExecuteQueryData 为 表数据查询场景
     *
     * @param sessionId
     * @return
     */
    @ApiOperation(value = "asyncSqlExecute", notes = "异步执行sql")
    @RequestMapping(value = {"/sessions/{sessionId}/sqls/asyncExecute"}, method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<SqlAsyncExecuteResp> asyncSqlExecute(@PathVariable String sessionId,
            @RequestBody SqlAsyncExecuteReq req) throws Exception {
        return Responses.success(consoleService.execute(SidUtils.getSessionId(sessionId), req));
    }

    @RequestMapping(value = {"/sessions/{sessionId}/sqls/streamExecute"}, method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<SqlAsyncExecuteResp> streamExecute(@PathVariable String sessionId,
            @RequestBody SqlAsyncExecuteReq req) throws Exception {
        return Responses.success(consoleService.streamExecute(SidUtils.getSessionId(sessionId), req, true));
    }

    /**
     * 获取异步执行sql的结果 Todo 这里的sqlIds后续需要改成一个string类型的requestId，异步api请求需要有超时机制
     *
     * @param sessionId
     * @return
     */
    @ApiOperation(value = "getAsyncSqlExecute", notes = "异步执行获取结果sql")
    @RequestMapping(value = "/sessions/{sessionId}/sqls/getResult", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<List<SqlExecuteResult>> getAsyncSqlExecute(@PathVariable String sessionId,
            @RequestParam String requestId) {
        return Responses.success(consoleService.getAsyncResult(SidUtils.getSessionId(sessionId), requestId, null));
    }

    @RequestMapping(value = "/sessions/{sessionId}/sqls/getMoreResults", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<AsyncExecuteResultResp> getMoreResults(@PathVariable String sessionId,
            @RequestParam String requestId) {
        return Responses.success(consoleService.getMoreResults(SidUtils.getSessionId(sessionId), requestId));
    }

    /**
     * 对 sql 脚本的内容进行静态检查
     *
     * @param sessionId
     * @return
     */
    @ApiOperation(value = "sqlCheck", notes = "连接内对 sql 脚本的内容进行静态检查")
    @RequestMapping(value = "/sessions/{sessionId}/sqlCheck", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<CheckResult> check(@PathVariable String sessionId, @RequestBody SqlCheckReq req) {
        ConnectionSession connectionSession = sessionService.nullSafeGet(SidUtils.getSessionId(sessionId), true);
        return Responses.list(this.sqlCheckService.check(connectionSession, req));
    }

    /**
     * 对多个数据库进行sql检查 todo 待完善
     * 
     * @param req
     * @return
     */
    @ApiOperation(value = "sqlCheck", notes = "statically check the contents of multiple sql scripts")
    @PostMapping("sessions/sqlCheck")
    public ListResponse<CheckResult> multipleCheck(@RequestBody MultipleSqlCheckReq req) {
        throw new NotImplementedException("Unsupported now");
    }

    /**
     * 获取异步执行sql的结果
     *
     * @param sessionId
     * @param format max size of the content, unit: KB
     * @return
     */
    @ApiOperation(value = "getBinaryContent", notes = "大字段内容查看接口")
    @RequestMapping(value = "/sessions/{sessionId}/sqls/{sqlId}/content", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<BinaryContent> getBinaryContent(@PathVariable String sessionId, @PathVariable String sqlId,
            @RequestParam Long row, @RequestParam Integer col,
            @RequestParam(required = false, defaultValue = "0") @Min(0) Long skip,
            @RequestParam(required = false, defaultValue = "4") @Max(48 * 1024) Integer len,
            @RequestParam(required = false, defaultValue = "TXT") ValueEncodeType format) throws IOException {
        return Responses.success(consoleService.getBinaryContent(
                SidUtils.getSessionId(sessionId), sqlId, row, col, skip * 1024, len * 1024, format));
    }

    /**
     * 下载二进制对象数据
     *
     * @param sessionId
     * @return
     */
    @ApiOperation(value = "download", notes = "下载二进制对象数据")
    @RequestMapping(value = "/sessions/{sessionId}/sqls/{sqlId}/download", method = RequestMethod.GET)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ResponseEntity<InputStreamResource> download(@PathVariable String sessionId, @PathVariable String sqlId,
            @RequestParam Long row, @RequestParam Integer col) {
        return consoleService.downloadBinaryContent(SidUtils.getSessionId(sessionId), sqlId, row, col);
    }

    /**
     * 通用session级别的文件上传下载接口
     *
     * @param sessionId session id
     * @param file file to be uploaded
     * @return file name
     */
    @ApiOperation(value = "upload", notes = "session级别的通用文件上传接口，用于上传数据")
    @RequestMapping(value = "/sessions/{sessionId}/upload", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<String> upload(@PathVariable String sessionId, @RequestBody MultipartFile file)
            throws IOException {
        return Responses.success(sessionService.uploadFile(SidUtils.getSessionId(sessionId), file.getInputStream()));
    }

    /**
     * 停止执行sql接口
     *
     * @param sessionId session id
     * @return kill result
     */
    @ApiOperation(value = "kill query", notes = "停止执行sql接口")
    @RequestMapping(value = "/sessions/{sessionId}/killQuery", method = RequestMethod.PUT)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<Boolean> killQuery(@PathVariable String sessionId) {
        return Responses.success(consoleService.killCurrentQuery(SidUtils.getSessionId(sessionId)));
    }

    /**
     * kill session
     *
     * @param req
     * @return
     */
    @ApiOperation(value = "kill session", notes = "终止会话接口")
    @RequestMapping(value = "/sessions/killSession", method = RequestMethod.POST)
    public SuccessResponse<List<KillSessionResult>> killSession(@RequestBody KillSessionOrQueryReq req) {
        return Responses.success(consoleService.killSessionOrQuery(req));
    }

    /**
     * 关闭数据库会话，，对应 /api/v1/session/close/{sid}
     *
     * @param req request body holding session ids
     * @return
     */
    @ApiOperation(value = "closeSession", notes = "关闭数据库连接会话，sid示例：sid:1000-1")
    @RequestMapping(value = "/sessions", method = RequestMethod.DELETE)
    @StatefulRoute(multiState = true, stateManager = "connectSessionCloseStateManager",
            stateIdExpression = "#req.sessionIds")
    public SuccessResponse<Set<String>> closeSession(@RequestBody MultiSessionsReq req) {
        Set<String> sessionIds = req.getSessionIds().stream()
                .map(SidUtils::getSessionId).collect(Collectors.toSet());
        return Responses.single(sessionService.close(sessionIds, 60, TimeUnit.SECONDS));
    }

    @ApiOperation(value = "queryTableOrViewData", notes = "查询表或视图的数据")
    @RequestMapping(value = {"/sessions/{sessionId}/queryData"}, method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<SqlExecuteResult> queryTableOrViewData(@PathVariable String sessionId,
            @RequestBody QueryTableOrViewDataReq req) throws Exception {
        return Responses.success(consoleService.queryTableOrViewData(SidUtils.getSessionId(sessionId), req));
    }

    @ApiOperation(value = "currentSessionStatus", notes = "查询当前数据库 Session 状态")
    @GetMapping(value = {"/sessions/{sessionId}/status"})
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public SuccessResponse<DBSessionResp> currentSessionStatus(@PathVariable String sessionId) {
        return Responses.success(sessionService.currentDBSession(sessionId));
    }

    @PostMapping(value = "/sessions/{sessionId}/partitionPlans/latest/preview")
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sessionId")
    public ListResponse<PartitionPlanPreViewResp> preview(@PathVariable String sessionId,
            @RequestBody PartitionPlanPreviewReq req) {
        return Responses.list(this.partitionPlanService.generatePartitionDdl(
                SidUtils.getSessionId(sessionId), req.getTableConfigs(), req.isOnlyForPartitionName()));
    }

}
