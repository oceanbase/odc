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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oceanbase.odc.core.flow.model.FlowTaskResult;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.FlowStatus;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.common.util.WebResponseUtils;
import com.oceanbase.odc.service.flow.FlowInstanceService;
import com.oceanbase.odc.service.flow.FlowTaskInstanceService;
import com.oceanbase.odc.service.flow.model.BinaryDataResult;
import com.oceanbase.odc.service.flow.model.CreateFlowInstanceReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceApprovalReq;
import com.oceanbase.odc.service.flow.model.FlowInstanceDetailResp;
import com.oceanbase.odc.service.flow.model.FlowMetaInfo;
import com.oceanbase.odc.service.flow.model.QueryFlowInstanceParams;
import com.oceanbase.odc.service.flow.util.TaskLogFilenameGenerator;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.partitionplan.PartitionPlanScheduleService;
import com.oceanbase.odc.service.partitionplan.model.PartitionPlanConfig;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;
import com.oceanbase.odc.service.task.model.OdcTaskLogLevel;

import io.swagger.annotations.ApiOperation;

/**
 * 流程实例【任务】管理
 *
 * @author wenniu.ly
 * @date 2022/1/25
 * @since ODC_release_3.3.0
 */

@RestController
@RequestMapping("/api/v2/flow/flowInstances")
public class FlowInstanceController {

    @Autowired
    private FlowInstanceService flowInstanceService;
    @Autowired
    private FlowTaskInstanceService flowTaskInstanceService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    @Autowired
    private PartitionPlanScheduleService partitionPlanScheduleService;

    @ApiOperation(value = "createFlowInstance", notes = "创建流程实例，返回流程实例")
    @RequestMapping(value = "/", method = RequestMethod.POST)
    public ListResponse<FlowInstanceDetailResp> createFlowInstance(@RequestBody CreateFlowInstanceReq flowInstanceReq) {
        flowInstanceReq.validate();
        if (authenticationFacade.currentUser().getOrganizationType() == OrganizationType.INDIVIDUAL) {
            return Responses.list(flowInstanceService.createIndividualFlowInstance(flowInstanceReq));
        } else {
            return Responses.list(flowInstanceService.create(flowInstanceReq));
        }
    }

    @ApiOperation(value = "listFlowInstances", notes = "获取流程实例列表")
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public PaginatedResponse<FlowInstanceDetailResp> listFlowInstances(
            @PageableDefault(size = Integer.MAX_VALUE, sort = {"id"}, direction = Direction.DESC) Pageable pageable,
            @RequestParam(required = false, name = "connectionId") List<Long> connectionIds,
            @RequestParam(required = false, name = "schema") String schema,
            @RequestParam(required = false, name = "creator") String creator,
            @RequestParam(required = false, name = "fuzzySearchKeyword") String fuzzySearchKeyword,
            @RequestParam(required = false, name = "status") List<FlowStatus> status,
            @RequestParam(required = false, name = "taskType") TaskType type,
            @RequestParam(required = false, name = "startTime") Date startTime,
            @RequestParam(required = false, name = "endTime") Date endTime,
            @RequestParam(name = "createdByCurrentUser") Boolean createdByCurrentUser,
            @RequestParam(name = "approveByCurrentUser") Boolean approveByCurrentUser,
            @RequestParam(required = false, name = "containsAll", defaultValue = "false") Boolean containsAll,
            @RequestParam(required = false, name = "parentInstanceId") Long parentInstanceId,
            @RequestParam(required = false, name = "projectId") Long projectId) {
        QueryFlowInstanceParams params = QueryFlowInstanceParams.builder()
                .connectionIds(connectionIds)
                .id(fuzzySearchKeyword)
                .statuses(status)
                .databaseName(schema)
                .creator(creator)
                .type(type)
                .startTime(startTime)
                .endTime(endTime)
                .createdByCurrentUser(createdByCurrentUser)
                .approveByCurrentUser(approveByCurrentUser)
                .containsAll(containsAll)
                .parentInstanceId(parentInstanceId)
                .projectId(projectId)
                .build();
        return Responses.paginated(flowInstanceService.list(pageable, params));
    }

    @ApiOperation(value = "detailFlowInstance", notes = "获取指定流程实例")
    @RequestMapping(value = "/{id:[\\d]+}", method = RequestMethod.GET)
    public SuccessResponse<FlowInstanceDetailResp> detailFlowInstance(@PathVariable Long id) {
        return Responses.single(flowInstanceService.detail(id));
    }

    @ApiOperation(value = "approveFlowInstance", notes = "审批通过指定流程实例")
    @RequestMapping(value = "/{id:[\\d]+}/approve", method = RequestMethod.POST)
    public SuccessResponse<FlowInstanceDetailResp> approveFlowInstance(@PathVariable Long id,
            @RequestBody FlowInstanceApprovalReq flowInstanceApprovalReq) throws IOException {
        return Responses.single(flowInstanceService.approve(id, flowInstanceApprovalReq.getComment(), false));
    }

    @ApiOperation(value = "rejectFlowInstance", notes = "审批拒绝指定流程实例")
    @RequestMapping(value = "/{id:[\\d]+}/reject", method = RequestMethod.POST)
    public SuccessResponse<FlowInstanceDetailResp> rejectFlowInstance(@PathVariable Long id,
            @RequestBody FlowInstanceApprovalReq flowInstanceApprovalReq) {
        return Responses.single(flowInstanceService.reject(id, flowInstanceApprovalReq.getComment(), false));
    }

    @ApiOperation(value = "cancelFlowInstance", notes = "终止流程")
    @RequestMapping(value = "/{id:[\\d]+}/cancel", method = RequestMethod.POST)
    public SuccessResponse<FlowInstanceDetailResp> cancelFlowInstance(@PathVariable Long id) {
        return Responses.single(flowInstanceService.cancel(id, false));
    }

    @ApiOperation(value = "executeFlowInstanceTask", notes = "手动执行流程任务")
    @RequestMapping(value = "/{id:[\\d]+}/tasks/execute", method = RequestMethod.POST)
    public SuccessResponse<FlowInstanceDetailResp> executeFlowInstanceTask(@PathVariable Long id) throws IOException {
        return Responses.single(flowTaskInstanceService.executeTask(id));
    }

    @ApiOperation(value = "rollbackFlowInstanceTask", notes = "回滚流程任务")
    @RequestMapping(value = "/{id:[\\d]+}/tasks/rollback", method = RequestMethod.POST)
    public SuccessResponse<FlowInstanceDetailResp> rollbackFlowInstanceTask(@PathVariable Long id) {
        throw new UnsupportedOperationException("Unsupported Operation");
    }

    @ApiOperation(value = "getLog", notes = "获取任务日志")
    @RequestMapping(value = "/{id:[\\d]+}/tasks/log", method = RequestMethod.GET)
    public SuccessResponse<String> getLog(@PathVariable Long id, @RequestParam OdcTaskLogLevel logType)
            throws IOException {
        return Responses.single(flowTaskInstanceService.getLog(id, logType));
    }

    @ApiOperation(value = "downloadLog", notes = "下载任务完整日志")
    @RequestMapping(value = "/{id:[\\d]+}/tasks/log/download", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadLog(@PathVariable Long id) throws IOException {
        List<BinaryDataResult> results = flowTaskInstanceService.downloadLog(id);
        PreConditions.validExists(ResourceType.ODC_FILE, "id", id, () -> CollectionUtils.isNotEmpty(results));
        return WebResponseUtils.getFileAttachmentResponseEntity(
                new InputStreamResource(results.get(0).getInputStream()), TaskLogFilenameGenerator.generate(id));
    }

    @ApiOperation(value = "getMetaInfo", notes = "获取流程相关的一些元数据信息，包括待审批数量等")
    @RequestMapping(value = "/getMetaInfo", method = RequestMethod.GET)
    public SuccessResponse<FlowMetaInfo> getMetaInfo() {
        return Responses.single(flowInstanceService.getMetaInfo());
    }

    @ApiOperation(value = "getResult", notes = "获取任务结果")
    @RequestMapping(value = "/{id:[\\d]+}/tasks/result", method = RequestMethod.GET)
    public ListResponse<? extends FlowTaskResult> getResult(@PathVariable Long id) throws IOException {
        return Responses.list(flowTaskInstanceService.getResult(id));
    }

    @ApiOperation(value = "getResult", notes = "获取任务结果")
    @RequestMapping(value = "/{flowInstanceId:[\\d]+}/tasks/{nodeInstanceId:[\\d]+}/result", method = RequestMethod.GET)
    public ListResponse<? extends FlowTaskResult> getResult(
            @PathVariable Long flowInstanceId, @PathVariable Long nodeInstanceId) throws IOException {
        return Responses.list(flowTaskInstanceService.getResult(flowInstanceId, nodeInstanceId));
    }

    @ApiOperation(value = "download", notes = "下载任务数据，仅用于 模拟数据、导出、数据库变更 任务")
    @RequestMapping(value = "/{id:[\\d]+}/tasks/download", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> download(@PathVariable Long id,
            @RequestParam(required = false) String fileName) throws IOException {
        List<BinaryDataResult> results = flowTaskInstanceService.download(id, fileName);
        PreConditions.validExists(ResourceType.ODC_FILE, "id", id, () -> CollectionUtils.isNotEmpty(results));
        return WebResponseUtils.getFileAttachmentResponseEntity(
                new InputStreamResource(results.get(0).getInputStream()), (results.get(0).getName()));
    }

    @ApiOperation(value = "downloadRollbackPlan", notes = "下载自动生成的回滚脚本文件")
    @RequestMapping(value = "/{id:[\\d]+}/tasks/rollbackPlan/download", method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadRollbackPlan(@PathVariable Long id) throws IOException {
        List<BinaryDataResult> results = flowTaskInstanceService.downRollbackPlanResult(id);
        PreConditions.validExists(ResourceType.ODC_FILE, "id", id, () -> CollectionUtils.isNotEmpty(results));
        return WebResponseUtils.getFileAttachmentResponseEntity(
                new InputStreamResource(results.get(0).getInputStream()), (results.get(0).getName()));
    }

    @ApiOperation(value = "status", notes = "获取实例的状态信息")
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public SuccessResponse<Map<Long, FlowStatus>> status(@RequestParam(name = "id") Set<Long> ids) {
        return Responses.success(flowInstanceService.getStatus(ids));
    }

    @ApiOperation(value = "asyncExecuteResult", notes = "获取异步任务预览结果")
    @RequestMapping(value = "/{id:[\\d]+}/tasks/asyncExecuteResult", method = RequestMethod.GET)
    public ListResponse<SqlExecuteResult> getQueryResult(@PathVariable Long id) throws IOException {
        return Responses.list(flowTaskInstanceService.getExecuteResult(id));
    }

    @RequestMapping(value = "/{id:[\\d]+}/tasks/{bucket}/batchGetDownloadUrl", method = RequestMethod.POST)
    public ListResponse<String> getDownloadUrl(@PathVariable Long id, @RequestBody List<String> objectId,
            @PathVariable String bucket) {
        return Responses.list(flowTaskInstanceService.getAsyncDownloadUrl(id, objectId, bucket));
    }

    @GetMapping(value = "/{id:[\\d]+}/tasks/partitionPlans/getDetail")
    public SuccessResponse<PartitionPlanConfig> getPartitionPlan(@PathVariable Long id) {
        return Responses.ok(this.partitionPlanScheduleService.getPartitionPlanByFlowInstanceId(id));
    }

}
