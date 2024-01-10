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
package com.oceanbase.odc.service.notification.model;

import java.util.Date;
import java.util.List;

import com.oceanbase.odc.metadb.notification.MessageEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: Lebie
 * @Date: 2023/3/20 14:44
 * @Description: []
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private Long id;
    private String title;
    private String content;
    private List<String> toRecipients;
    private List<String> ccRecipients;
    private MessageSendingStatus status;
    private Integer retryTimes;
    private Integer maxRetryTimes;
    private Long creatorId;
    private Long organizationId;
    private Long projectId;
    private String errorMessage;
    private Date createTime;
    private Date updateTime;
    private Date lastSentTime;
    private Channel channel;


    public MessageEntity toEntity() {
        MessageEntity entity = new MessageEntity();
        entity.setId(this.getId());
        entity.setTitle(this.getTitle());
        entity.setContent(this.getContent());
        entity.setCreatorId(this.getCreatorId());
        entity.setOrganizationId(this.getOrganizationId());
        entity.setProjectId(this.projectId);
        entity.setChannelId(this.channel.getId());
        entity.setStatus(this.getStatus());
        entity.setToRecipients(this.getToRecipients());
        entity.setCcRecipients(this.getCcRecipients());
        entity.setRetryTimes(this.getRetryTimes());
        entity.setMaxRetryTimes(this.getMaxRetryTimes());
        return entity;
    }

    public static Message fromEntity(MessageEntity entity) {
        Message message = new Message();
        message.setId(entity.getId());
        message.setContent(entity.getContent());
        message.setTitle(entity.getTitle());
        message.setCcRecipients(entity.getCcRecipients());
        message.setToRecipients(entity.getToRecipients());
        message.setStatus(entity.getStatus());
        message.setCreatorId(entity.getCreatorId());
        message.setOrganizationId(entity.getOrganizationId());
        message.setProjectId(entity.getProjectId());
        message.setRetryTimes(entity.getRetryTimes());
        message.setMaxRetryTimes(entity.getMaxRetryTimes());
        return message;
    }
}
