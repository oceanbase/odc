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

package com.oceanbase.odc.service.datasecurity.model;

import java.util.List;

import com.oceanbase.odc.service.connection.database.model.Database;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/8/29 16:46
 */
@Data
public class SensitiveColumnStats {

    /**
     * Sensitive column related databases
     */
    private List<Database> databases;
    /**
     * Sensitive column related masking algorithms
     */
    private List<MaskingAlgorithm> maskingAlgorithms;

}
