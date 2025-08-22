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

import lombok.Data;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/31
 */
@Data
public class SqlCopilotReq {
    /**
     * 数据库唯一标识，connect_database表的ID
     */
    Long databaseId;
    /**
     * 文件内容
     */
    String fileContent;
    /**
     * 选中内容起始位置
     */
    Integer startPosition;
    /**
     * 选中内容结束位置
     */
    Integer endPosition;
    /**
     * 用户输入
     */
    String input;
    /**
     * 用户提问类型
     */
    QuestionType questionType;
    /**
     * 会话ID
     */
    String sid;
    /**
     * 是否是流式请求，这期不用管，都是流式
     */
    boolean stream;
    /**
     * 使用的模型，为空表示使用默认模型。示例：TONGYI/qwen-max
     */
    String model;
    /**
     * SQL语句分隔符
     */
    String delimiter;
    /**
     * 当前光标位置
     */
    Integer cursorPosition;
}
