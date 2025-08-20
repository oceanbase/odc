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

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/7/23
 */
public enum QuestionType {

    /**
     * SQL 优化
     */
    SQL_OPTIMIZER,
    /**
     * SQL 纠错
     */
    SQL_DEBUGGING,
    /**
     * Text2Sql
     */
    NL_2_SQL,
    /**
     * SQL 修改
     */
    SQL_MODIFIER,
    /**
     * SQL 格式化
     */
    SQL_FORMATTING,

    /**
     * SQL 补全
     */
    SQL_COMPLETION;

}
