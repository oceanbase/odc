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
package com.oceanbase.odc.service.task.runtime;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.regulation.risklevel.model.RiskLevelDescriber;
import com.oceanbase.odc.service.regulation.ruleset.model.Rule;

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
     * The environment of the target database
     */
    private Environment environment;
    /**
     * The rules to be checked according to the environment
     */
    private List<Rule> rules;
    /**
     * The connection config of target database
     */
    private ConnectionConfig connectionConfig;
    /**
     * The authorized database names according to the connectionConfig
     */
    private Set<String> authorizedDatabaseNames;
    /**
     * The object storage metadata of the pre-check SQL file
     */
    private List<ObjectMetadata> sqlFileObjectMetadatas;

}
