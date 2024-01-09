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
package com.oceanbase.odc.metadb.notification;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.common.jpa.JsonListConverter;
import com.oceanbase.odc.service.notification.model.MessageSendingStatus;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2023/4/7 15:42
 * @Description: []
 */
@Data
@Entity
@Table(name = "notification_message")
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;
    @Column(name = "creator_id", nullable = false, updatable = false)
    private Long creatorId;
    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;
    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;
    @Column(name = "title", nullable = false, updatable = false)
    private String title;
    @Column(name = "content", nullable = false, updatable = false)
    private String content;
    @Convert(converter = JsonListConverter.class)
    @Column(name = "to_recipients", updatable = false)
    private List<String> toRecipients;
    @Convert(converter = JsonListConverter.class)
    @Column(name = "cc_recipients", updatable = false)
    private List<String> ccRecipients;
    @Column(name = "channel_id", nullable = false, updatable = false)
    private Long channelId;
    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false)
    private MessageSendingStatus status;
    @Column(name = "retry_times", nullable = false, updatable = false)
    private Integer retryTimes;
    @Column(name = "max_retry_times", nullable = false, updatable = false)
    private Integer maxRetryTimes;
    @Column(name = "last_sent_time")
    private Date lastSentTime;
    @Column(name = "error_message")
    private String errorMessage;
}
