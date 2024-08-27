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
package com.oceanbase.odc.service.task.base.precheck;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2024/1/30 11:06
 */
@Data
public class PreCheckTaskParameters implements Serializable {

    private static final long serialVersionUID = -920282926953327148L;

    /**
     * The maximum number of bytes to read from the pre-check SQL file
     */
    private Long maxReadContentBytes = 5 * 1024 * 1024L;
    /**
     * The type of the task, used to deserialize the parameter json string
     */
    private TaskType taskType;
    /**
     * The raw parameter json string of the task
     */
    private String parameterJson;
    /**
     * The risk level describer
     */
    private RiskLevelDescriber riskLevelDescriber;
    /**
     * The rules to be checked according to the environment
     */
    private List<Rule> rules;
    /**
     * The connection config of target database
     */
    private ConnectionConfig connectionConfig;
    /**
     * The default schema of the task
     */
    private String defaultSchema;
    /**
     * The authorized databases and their permissions of the connection config
     */
    private List<AuthorizedDatabase> authorizedDatabase;
    /**
     * The object storage metadata of the pre-check SQL file
     */
    private List<ObjectMetadata> sqlFileObjectMetadatas;

    @Data
    @AllArgsConstructor
    public static class AuthorizedDatabase implements Serializable {

        private static final long serialVersionUID = -4535323031261488874L;

        private Long id;
        private String name;
        private Set<DatabasePermissionType> permissionTypes;

    }

}
