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
public enum DBMViewSyncDataMethod {

    REFRESH_FAST("REFRESH FAST", "f"),
    REFRESH_FORCE("REFRESH FORCE", "?"),
    REFRESH_COMPLETE("REFRESH COMPLETE", "c"),
    REFRESH_ALWAYS("REFRESH ALWAYS", "a"),
    NEVER_REFRESH("NEVER REFRESH", "n");

    private String name;
    private String value;

    DBMViewSyncDataMethod(String name, String value) {
        this.name = name;
        this.value = value;
    }

}
