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
package com.oceanbase.odc.service.onlineschemachange.rename;

import java.util.List;

import com.oceanbase.odc.service.onlineschemachange.model.OriginTableCleanStrategy;

import lombok.Builder;
import lombok.Data;

/**
 * @author yaobin
 * @date 2023-08-03
 * @since 4.2.0
 */
@Data
@Builder
public class RenameTableParameters {

    private String schemaName;

    private String originTableName;

    private String renamedTableName;

    private String newTableName;

    private Integer lockTableTimeOutSeconds;

    private OriginTableCleanStrategy originTableCleanStrategy;

    private List<String> lockUsers;
}
