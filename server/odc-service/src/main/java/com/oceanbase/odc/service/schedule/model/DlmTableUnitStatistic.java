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
package com.oceanbase.odc.service.schedule.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

/**
 * @Author：tinker
 * @Date: 2024/5/9 09:39
 * @Descripition:
 */
@Data
public class DlmTableUnitStatistic {

    private Long processedRowCount = 0L;

    private Long readRowCount = 0L;

    private Long processedRowsPerSecond = 0L;

    private Long readRowsPerSecond = 0L;

    private String globalMinKey;

    private String globalMaxKey;

    private Map<String, String> partName2MinKey = new HashMap<>();

    private Map<String, String> partName2MaxKey = new HashMap<>();

}
