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
package com.oceanbase.odc.service.notification.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageSendingStatus {

    CREATED("CREATED"),

    SENT_SUCCESSFULLY("SENT_SUCCESSFULLY"),

    SENT_FAILED("SENT_FAILED"),

    THROWN("THROWN"),

    SENDING("SENDING");

    private String name;

    MessageSendingStatus(String name) {
        this.name = name;
    }

    @JsonValue
    public String getValue() {
        return this.name;
    }
}
