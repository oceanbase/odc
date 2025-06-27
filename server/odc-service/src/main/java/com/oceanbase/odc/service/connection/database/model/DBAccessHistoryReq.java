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
package com.oceanbase.odc.service.connection.database.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @Author: ysj
 * @Date: 2025/2/24 13:36
 * @Since: 4.3.4
 * @Description: database access history req
 */
@Data
@Accessors(chain = true)
public class DBAccessHistoryReq {

    @NotNull(message = "Query history count param must not be null")
    @Min(value = 1, message = "Query history count must greater than or equals 1")
    private Integer historyCount;
}
