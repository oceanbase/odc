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
package com.oceanbase.odc.service.ai.knowledgebase.utils;

import java.util.ArrayList;
import java.util.List;

import com.oceanbase.odc.service.ai.Constants;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/4
 */
public class ChatUtils {

    public static List<ChatMessage> buildChatMessages(String system, String user) {
        List<ChatMessage> chatMessages = new ArrayList<>();
        if (system != null) {
            chatMessages.add(new SystemMessage(system));
        }
        if (user != null) {
            chatMessages.add(new UserMessage(user));
        }
        return chatMessages;
    }

    public static String removeSurroundCharacter(String message) {
        message = message.trim();
        // 补全的prompt中要求返回值被<<>>围绕，返回时将其去掉
        if (message.startsWith(Constants.PRE_SURROUND)) {
            message = message.substring(2);
        }
        if (message.endsWith(Constants.POST_SURROUND)) {
            message = message.substring(0, message.length() - 2);
        }
        return message;
    }

}
