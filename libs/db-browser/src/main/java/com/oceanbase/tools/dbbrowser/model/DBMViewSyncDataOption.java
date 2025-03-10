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
public enum DBMViewSyncDataOption {

    FAST_REFRESH("f"),
    FORCE_REFRESH("?"),
    COMPLETE_REFRESH("c"),
    ALWAYS_REFRESH("a"),
    NEVER_REFRESH("n");

    private String value;

    DBMViewSyncDataOption(String value) {
        this.value = value;
    }

}
