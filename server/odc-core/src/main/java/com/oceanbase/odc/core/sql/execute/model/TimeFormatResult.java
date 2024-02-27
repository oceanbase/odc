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
package com.oceanbase.odc.core.sql.execute.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class TimeFormatResult {

    private Long timestamp;
    private Integer nano;
    private String timeZoneId;
    private String formattedContent;

    public TimeFormatResult(@NonNull String formattedContent) {
        this.formattedContent = formattedContent;
    }

    public TimeFormatResult(@NonNull String formattedContent, @NonNull Long timestamp) {
        this.timestamp = timestamp;
        this.formattedContent = formattedContent;
    }

    public TimeFormatResult(@NonNull String formattedContent, @NonNull Long timestamp,
            @NonNull Integer nano, String timeZoneId) {
        this.timestamp = timestamp;
        this.formattedContent = formattedContent;
        this.nano = nano;
        this.timeZoneId = timeZoneId;
    }

    @Override
    public String toString() {
        return this.formattedContent;
    }

}
