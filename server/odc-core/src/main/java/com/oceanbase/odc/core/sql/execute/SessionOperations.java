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
import java.util.List;
import java.util.Map;

import lombok.NonNull;

/**
 * @author yaobin
 * @date 2023-04-25
 * @since 4.2.0
 */
public interface SessionOperations {

    String getConnectionId(@NonNull Connection connection);

    void killQuery(@NonNull Connection connection, @NonNull String connectionId);

    /**
     * Get kill query SQL by connectionId
     *
     * @param connectionIds
     * @return the map of connectionId to kill query SQL
     */
    Map<String, String> getKillQuerySqls(@NonNull List<String> connectionIds);


    /**
     * Get kill session SQL by connectionId
     *
     * @param connectionIds
     * @return the map of connectionId to kill session SQL
     */
    Map<String, String> getKillSessionSqls(@NonNull List<String> connectionIds);

}
