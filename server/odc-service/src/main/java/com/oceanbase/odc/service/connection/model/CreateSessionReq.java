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

package com.oceanbase.odc.service.connection.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link CreateSessionReq}
 *
 * @author yh263208
 * @date 2023-11-15 21:06
 * @since ODC_release_4.2.3
 * @see java.io.Serializable
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class CreateSessionReq implements Serializable {

    private String schema;
    @NotNull
    private Long dsId;
    private String realId;

    public static CreateSessionReq from(@NonNull ConnectionConfig connectionConfig) {
        return new CreateSessionReq(connectionConfig.getId(), null, connectionConfig.getDefaultSchema());
    }

    public CreateSessionReq(@NonNull Long dsId, String realId, String schema) {
        this.schema = schema;
        this.dsId = dsId;
        this.realId = realId;
    }

}
