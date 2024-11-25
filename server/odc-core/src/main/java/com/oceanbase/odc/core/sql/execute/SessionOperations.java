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
package com.oceanbase.odc.core.sql.execute;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-04-25
 * @since 4.2.0
 */
public interface SessionOperations {

    String getConnectionId(@NonNull Connection connection);

    void killQuery(@NonNull Connection connection, @NonNull String connectionId);

    String getKillQuerySql(@NonNull String connectionId);

    String getKillSessionSql(@NonNull String connectionId);

    /**
     * Get kill query SQL by connectionId
     *
     * @param connectionIds
     * @return the map of connectionId to kill query SQL
     */
    default Map<String, String> getKillQuerySqls(@NotEmpty Set<String> connectionIds) {
        return connectionIds.stream().collect(Collectors.toMap(id -> id, this::getKillQuerySql));
    }


    /**
     * Get kill session SQL by connectionId
     *
     * @param connectionIds
     * @return the map of connectionId to kill session SQL
     */
    default Map<String, String> getKillSessionSqls(@NonNull Set<String> connectionIds) {
        return connectionIds.stream().collect(Collectors.toMap(id -> id, this::getKillSessionSql));
    }

}
