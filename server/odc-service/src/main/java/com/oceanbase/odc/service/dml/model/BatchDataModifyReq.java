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
package com.oceanbase.odc.service.dml.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author yizhou.xw
 * @version : OdcBatchDataModifyReq.java, v 0.1 2021-08-19 13:16
 */
@Data
public class BatchDataModifyReq {
    private String schemaName;

    @NotBlank(message = "tableName cannot be null or blank")
    private String tableName;

    @Valid
    @NotEmpty(message = "rows cannot be null or empty")
    private List<Row> rows;

    private List<String> whereColumns;

    @Data
    public static class Row {
        @NotNull(message = "operate cannot be null")
        private Operate operate;

        @Valid
        @NotEmpty(message = "units cannot be null or empty")
        private List<DataModifyUnit> units;
    }

    public enum Operate {
        INSERT,
        UPDATE,
        DELETE,;
    }
}
