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
package com.oceanbase.tools.dbbrowser.model;

import lombok.Getter;

/**
 * @description:
 * @author: zijia.cj
 * @date: 2025/3/4 14:53
 * @since: 4.3.4
 */
@Getter
public enum DBMaterializedViewRefreshMethod {

    REFRESH_FAST("REFRESH FAST", "FAST", "f"),
    REFRESH_FORCE("REFRESH FORCE", "FORCE", "?"),
    REFRESH_COMPLETE("REFRESH COMPLETE", "COMPLETE", "c"),
    REFRESH_ALWAYS("REFRESH ALWAYS", "ALWAYS", "a"),
    NEVER_REFRESH("NEVER REFRESH", "NEVER", "n"),
    OTHERS("UNKNOWN", "UNKNOWN", "UNKNOWN");

    private String createName;
    private String showName;
    private String value;

    DBMaterializedViewRefreshMethod(String createName, String showName, String value) {
        this.createName = createName;
        this.showName = showName;
        this.value = value;
    }

    public static DBMaterializedViewRefreshMethod getEnumByShowName(String name) {
        DBMaterializedViewRefreshMethod result = DBMaterializedViewRefreshMethod.OTHERS;
        DBMaterializedViewRefreshMethod[] methods = DBMaterializedViewRefreshMethod.values();
        for (DBMaterializedViewRefreshMethod method : methods) {
            if (method.getShowName().equalsIgnoreCase(name)) {
                result = method;
                break;
            }
        }
        return result;
    }

}
