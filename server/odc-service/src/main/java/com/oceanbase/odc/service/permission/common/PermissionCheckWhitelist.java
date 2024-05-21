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
package com.oceanbase.odc.service.permission.common;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import com.oceanbase.odc.core.shared.constant.DialectType;

import lombok.Data;
import lombok.NonNull;

/**
 * @author gaoda.xy
 * @date 2024/5/16 15:38
 */
@Data
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "odc.permission-check.whitelist")
public class PermissionCheckWhitelist {

    private Map<String, Set<String>> database;

    @PostConstruct
    private void init() {
        if (database != null) {
            for (Map.Entry<String, Set<String>> entry : database.entrySet()) {
                Set<String> caseInsensitiveSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                caseInsensitiveSet.addAll(entry.getValue());
                database.put(entry.getKey(), caseInsensitiveSet);
            }
        }
    }

    public Set<String> getDatabaseWhitelist(@NonNull DialectType dialect) {
        if (database == null || database.isEmpty()) {
            return new TreeSet<>();
        }
        String key = dialect.name().toLowerCase().replace("_", "-");
        return database.getOrDefault(key, new TreeSet<>());
    }

    public boolean containsDatabase(@NonNull String database, @NonNull DialectType dialect) {
        return getDatabaseWhitelist(dialect).contains(database);
    }

}
