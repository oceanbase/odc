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

import java.util.Date;

import com.oceanbase.tools.migrator.common.configure.LogicTableConfig;

/**
 * @Author：tinker
 * @Date: 2024/5/10 11:13
 * @Descripition:
 */
public class DataArchiveTaskShardParameters implements TaskShardParameters {

    private String tableName;

    private Date fireTime;

    private Long sourceDatabaseId;

    private Long targetDatabaseId;

    private LogicTableConfig logicTableConfig;

}
