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
package com.oceanbase.odc.service.ai.chat.model;

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Chat {

    private Long id;
    private Long databaseId;
    private Long creatorId;
    private String conversationId;
    private QuestionType chatType;
    private String reference;
    private String input;
    private String content;
    private FeedbackResult feedbackResult;
    private String feedbackContent;
    private ChatStatus status;
    private String errorMessage;
    private Date createTime;
    private String obCloudOrganizationName;
    private String obCloudProjectName;
    private String databaseName;
    private DialectType dialectType;
    private List<Entry<String, String>> stages;
    private String requestId;

    public Chat(String content, ChatStatus status) {
        this.content = content;
        this.status = status;
    }

}
