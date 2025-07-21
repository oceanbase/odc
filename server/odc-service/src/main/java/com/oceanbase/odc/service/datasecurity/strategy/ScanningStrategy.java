/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.odc.service.datasecurity.strategy;

import java.util.List;
import java.util.Map;

import com.oceanbase.odc.service.datasecurity.model.ScanResult;
import com.oceanbase.odc.service.datasecurity.recognizer.ColumnRecognizer;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

/**
 * 敏感列扫描策略接口
 * 
 * @author Assistant
 * @date 2025/1/27
 */
public interface ScanningStrategy {

    /**
     * 执行单个列的扫描
     *
     * @param column           待扫描的列
     * @param basicRecognizers 基础规则识别器列表
     * @param aiRecognizers    AI识别器列表
     * @return 扫描结果
     */
    ScanResult scan(DBTableColumn column, List<ColumnRecognizer> basicRecognizers,
            List<ColumnRecognizer> aiRecognizers);

    /**
     * 执行批量列的扫描
     *
     * @param columns          待扫描的列列表
     * @param basicRecognizers 基础规则识别器列表
     * @param aiRecognizers    AI识别器列表
     * @return 扫描结果映射，key为列标识符，value为扫描结果
     */
    Map<String, ScanResult> scanBatch(List<DBTableColumn> columns, List<ColumnRecognizer> basicRecognizers,
            List<ColumnRecognizer> aiRecognizers);
}