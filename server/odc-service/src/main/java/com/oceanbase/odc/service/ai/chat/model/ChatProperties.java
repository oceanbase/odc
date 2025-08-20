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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * @author: liuyizhuo.lyz
 * @date: 2025/8/1
 */
@Data
@Configuration
@RefreshScope
public class ChatProperties {

    @Value("${odc.copilot.llm.text2sql.max-table-count-to-llm:10}")
    private int text2SqlMaxTableCountToLLM;

    @Value("${odc.copilot.retrieve.vector-weight:1}")
    private Double vectorIndexRetrieveWeight;

    @Value("${odc.copilot.retrieve.kv-weight:1}")
    private Double kvIndexRetrieveWeight;

    @Value("${odc.copilot.llm.text2sql.feedback.times:1}")
    private int text2SqlFeedbackTimes;

    @Value("${odc.copilot.retrieve.topK:6}")
    private Integer retrieveTopK;

    @Value("${odc.copilot.retrieve.vector.table-retrieve-num:1}")
    private Integer vectorIndexTableRetrieveNum;

    @Value("${odc.copilot.retrieve.vector.field-retrieve-num:3}")
    private Integer vectorIndexFieldRetrieveNum;

    @Value("${odc.copilot.retrieve.vector.retrieve-min-score:0.2}")
    private Double vectorIndexRetrieveMinScore;

    @Value("${odc.copilot.retrieve.kv.field.num:5}")
    private Integer kvIndexFieldRetrieveNum;

    @Value("${odc.copilot.retrieve.kv.table.decrease:0.1}")
    private Double kvIndexFieldRetrieveScoreDecrease;

    @Value("${odc.copilot.retrieve.kv.table.score:1.0}")
    private Double kvIndexTableRetrieveScore;

    @Value("${odc.copilot.max-qps-per-user:2.0}")
    private Double maxQpsPerUser;

    @Value("${odc.copilot.session.context.max-lines:10}")
    private Integer sessionContextMaxLines;

    @Value("${odc.copilot.session.context.max-len-per-line:600}")
    private Integer sessionContextMaxLengthPerLine;

}
