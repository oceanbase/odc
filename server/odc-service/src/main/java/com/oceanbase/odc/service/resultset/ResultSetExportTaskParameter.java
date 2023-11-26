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
package com.oceanbase.odc.service.resultset;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oceanbase.odc.core.flow.model.TaskParameters;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.EncodingType;
import com.oceanbase.odc.service.datasecurity.model.MaskingAlgorithm;

import lombok.Data;

/**
 * @Author: Lebie
 * @Date: 2021/11/22 下午2:11
 * @Description: [result set export DTO]
 */
@Data
public class ResultSetExportTaskParameter implements Serializable, TaskParameters {
    private String sql;
    private String fileName;
    private DataTransferFormat fileFormat;
    private EncodingType fileEncoding;
    private String tableName;
    private Long maxRows;
    /**
     * whether save the original sql in another Excel Sheet, which only works when fileFormat is EXCEL
     */
    private boolean saveSql = true;
    private CSVFormat csvFormat;
    @JsonIgnore
    private List<MaskingAlgorithm> rowDataMaskingAlgorithms;
    @JsonIgnore
    private String database;

    @Data
    static class CSVFormat {
        @JsonProperty("isContainColumnHeader")
        boolean isContainColumnHeader = true;
        @JsonProperty("isTransferEmptyString")
        boolean isTransferEmptyString = true;
        char columnSeparator = ',';
        char columnDelimiter = '"';
        String lineSeparator = "\r\n";
    }
}
