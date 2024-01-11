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

import com.oceanbase.odc.service.common.response.ListResponse;
import com.oceanbase.odc.service.common.response.PaginatedResponse;
import com.oceanbase.odc.service.common.response.Responses;
import com.oceanbase.odc.service.common.response.SuccessResponse;
import com.oceanbase.odc.service.notification.NotificationService;
import com.oceanbase.odc.service.notification.model.Channel;
import com.oceanbase.odc.service.notification.model.ChannelType;
import com.oceanbase.odc.service.notification.model.Message;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;
import com.oceanbase.odc.service.notification.model.NotificationPolicy;
import com.oceanbase.odc.service.notification.model.QueryChannelParams;
import com.oceanbase.odc.service.notification.model.QueryMessageParams;
import com.oceanbase.odc.service.notification.model.TestChannelResult;

import io.swagger.annotations.ApiOperation;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/9
 */
@RestController
@RequestMapping("/api/v2/collaboration/projects/{projectId}/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @ApiOperation(value = "listChannels", notes = "List all channels in the project")
    @RequestMapping(value = "/channels", method = RequestMethod.GET)
    public PaginatedResponse<Channel> listChannels(@PathVariable Long projectId,
            @RequestParam(required = false, name = "name") String fuzzyChannelName,
            @RequestParam(required = false, name = "type") List<ChannelType> channelTypes,
            @PageableDefault(size = Integer.MAX_VALUE, sort = "id") Pageable pageable) {

        QueryChannelParams queryParams = QueryChannelParams.builder()
                .fuzzyChannelName(fuzzyChannelName)
                .channelTypes(channelTypes)
                .projectId(projectId).build();
        return Responses.paginated(notificationService.listChannels(projectId, queryParams, pageable));
    }

    @ApiOperation(value = "detailChannel", notes = "Detail a channel by id")
    @RequestMapping(value = "/channels/{id}", method = RequestMethod.GET)
    public SuccessResponse<Channel> detailChannel(@PathVariable Long projectId, @PathVariable Long channelId) {
        return Responses.success(notificationService.detailChannel(projectId, channelId));
    }

    @ApiOperation(value = "createChannel", notes = "Create a channel")
    @RequestMapping(value = "/channels", method = RequestMethod.POST)
    public SuccessResponse<Channel> createChannel(@PathVariable Long projectId, @RequestBody Channel channel) {
        return Responses.success(notificationService.createChannel(projectId, channel));
    }

    @ApiOperation(value = "updateChannel", notes = "Update a channel by id")
    @RequestMapping(value = "/channels/{id}", method = RequestMethod.PUT)
    public SuccessResponse<Channel> updateChannel(@PathVariable Long projectId, @RequestBody Channel channel) {
        return Responses.success(notificationService.updateChannel(projectId, channel));
    }

    @ApiOperation(value = "deleteChannel", notes = "Delete a channel by id")
    @RequestMapping(value = "/channels/{id}", method = RequestMethod.DELETE)
    public SuccessResponse<Channel> deleteChannel(@PathVariable Long projectId, @PathVariable Long channelId) {
        return Responses.success(notificationService.deleteChannel(projectId, channelId));
    }

    @ApiOperation(value = "testChannel", notes = "Test whether the channel is available")
    @RequestMapping(value = "/channels/test", method = RequestMethod.POST)
    public SuccessResponse<TestChannelResult> testChannel(@PathVariable Long projectId, @RequestBody Channel channel) {
        return Responses.success(notificationService.testChannel(channel));
    }

    @ApiOperation(value = "existsChannel", notes = "Query whether a channel name exists")
    @RequestMapping(value = "/channels/exists", method = RequestMethod.GET)
    public SuccessResponse<Boolean> existsChannel(@PathVariable Long projectId, @RequestParam String name) {
        return Responses.success(notificationService.existsChannel(projectId, name));
    }

    @ApiOperation(value = "listPolicies", notes = "List all policies in the project")
    @RequestMapping(value = "/policies", method = RequestMethod.GET)
    public ListResponse<NotificationPolicy> listPolicies(@PathVariable Long projectId) {
        return Responses.list(notificationService.listPolicies(projectId));
    }

    @ApiOperation(value = "detailPolicy", notes = "Detail a policy by id")
    @RequestMapping(value = "/policies/{id}", method = RequestMethod.GET)
    public SuccessResponse<NotificationPolicy> detailPolicy(@PathVariable Long projectId, @PathVariable Long policyId) {
        return Responses.success(notificationService.detailPolicy(projectId, policyId));
    }

    @ApiOperation(value = "batchUpdatePolicies", notes = "Batch update policies")
    @RequestMapping(value = "/policies", method = RequestMethod.PUT)
    public ListResponse<NotificationPolicy> batchUpdatePolicies(@PathVariable Long projectId,
            @RequestBody List<NotificationPolicy> policies) {
        return Responses.list(notificationService.batchUpdatePolicies(projectId, policies));
    }

    @ApiOperation(value = "listMessages", notes = "List messages in the project")
    @RequestMapping(value = "/messages", method = RequestMethod.GET)
    public PaginatedResponse<Message> listMessages(@PathVariable Long projectId,
            @RequestParam(required = false, name = "title") String fuzzyTitle,
            @RequestParam(required = false, name = "channelId") List<Long> channelIds,
            @RequestParam(required = false, name = "status") List<MessageSendingStatus> statuses,
            @PageableDefault(
                    size = Integer.MAX_VALUE, sort = {"createTime"}, direction = Direction.DESC) Pageable pageable) {
        QueryMessageParams queryParams = QueryMessageParams.builder()
                .fuzzyTitle(fuzzyTitle)
                .channelIds(channelIds)
                .statuses(statuses)
                .projectId(projectId).build();
        return Responses.paginated(notificationService.listMessages(projectId, queryParams, pageable));
    }

    @ApiOperation(value = "detailMessage", notes = "Detail a message by id")
    @RequestMapping(value = "/messages/{id}", method = RequestMethod.GET)
    public SuccessResponse<Message> detailMessage(@PathVariable Long projectId, @PathVariable Long messageId) {
        return Responses.success(notificationService.detailMessage(projectId, messageId));
    }

}
