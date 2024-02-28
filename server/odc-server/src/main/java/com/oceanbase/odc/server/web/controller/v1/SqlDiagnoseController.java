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

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.shared.model.SqlExecDetail;
import com.oceanbase.odc.core.shared.model.SqlExplain;
import com.oceanbase.odc.core.shared.model.TraceSpan;
import com.oceanbase.odc.service.common.model.ResourceSql;
import com.oceanbase.odc.service.common.response.OdcResult;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.SidUtils;
import com.oceanbase.odc.service.diagnose.SqlDiagnoseService;
import com.oceanbase.odc.service.session.ConnectSessionService;
import com.oceanbase.odc.service.state.model.StateName;
import com.oceanbase.odc.service.state.model.StatefulRoute;

import io.swagger.annotations.ApiOperation;

/**
 * @author
 */
@RestController

@RequestMapping("/api/v1/diagnose")
public class SqlDiagnoseController {

    @Autowired
    private SqlDiagnoseService diagnoseService;
    @Autowired
    private ConnectSessionService sessionService;

    @ApiOperation(value = "explain", notes = "对sql执行explain查看计划信息，sid示例：sid:1000-1:d:db1")
    @RequestMapping(value = "/explain/{sid}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<SqlExplain> explain(@PathVariable String sid, @RequestBody ResourceSql sql) {
        return OdcResult.ok(diagnoseService.explain(sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), sql));
    }

    @ApiOperation(value = "getExecExplain", notes = "查询实际执行的计划信息，sid示例：sid:1000-1:d:db1,"
            + "注意参数tag带上sql_id")
    @RequestMapping(value = "/getExecExplain/{sid}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<SqlExplain> getExecExplain(@PathVariable String sid, @RequestBody ResourceSql sql) {
        return OdcResult.ok(diagnoseService.getPhysicalPlan(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), sql));
    }

    @ApiOperation(value = "getExecDetail", notes = "查询执行sql的信息，例如等待时间、执行时间、IO等，sid示例：sid:1000-1:d:db1,"
            + "注意参数tag带上trace_id")
    @RequestMapping(value = "/getExecDetail/{sid}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public OdcResult<SqlExecDetail> getExecDetail(@PathVariable String sid, @RequestBody ResourceSql sql) {
        return OdcResult.ok(diagnoseService.getExecutionDetail(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid), true), sql));
    }

    @ApiOperation(value = "getFullLinkTrace", notes = "获取全链路诊断信息，嵌套数据结构")
    @RequestMapping(value = "/getFullLinkTrace/{sid}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<TraceSpan> getFullLinkTrace(@PathVariable String sid, @RequestBody ResourceSql sql)
            throws IOException {
        return Responses.success(diagnoseService.getFullLinkTrace(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid)), sql));
    }

    @ApiOperation(value = "getFullLinkTraceDownloadUrl")
    @RequestMapping(value = "/getFullLinkTraceDownloadUrl/{sid}", method = RequestMethod.POST)
    @StatefulRoute(stateName = StateName.DB_SESSION, stateIdExpression = "#sid")
    public SuccessResponse<String> getFullLinkTraceJson(@PathVariable String sid, @RequestBody ResourceSql sql)
            throws IOException {
        return Responses.success(diagnoseService.getFullLinkTraceDownloadUrl(
                sessionService.nullSafeGet(SidUtils.getSessionId(sid)), sql));
    }

}
