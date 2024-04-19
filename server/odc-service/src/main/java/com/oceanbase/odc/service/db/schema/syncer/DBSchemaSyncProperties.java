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
package com.oceanbase.odc.service.db.schema.syncer;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Data;
import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/4/17 15:36
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "odc.database.schema.sync")
public class DBSchemaSyncProperties {

    private String cronExpression;
    private int executorThreadCount;
    private boolean blockExclusionsWhenSyncDbToProject;
    private boolean blockExclusionsWhenSyncDbSchemas;
    private Map<String, List<String>> excludeSchemas;

    public List<String> getExcludeSchemas(@NonNull DialectType dialect) {
        if (excludeSchemas == null || excludeSchemas.isEmpty()) {
            return Collections.emptyList();
        }
        String key = dialect.name().toLowerCase().replace("_", "-");
        return excludeSchemas.getOrDefault(key, Collections.emptyList());
    }

}
