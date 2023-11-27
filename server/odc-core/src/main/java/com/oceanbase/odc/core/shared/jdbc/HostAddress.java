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
package com.oceanbase.odc.core.shared.jdbc;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link HostAddress}
 *
 * @author yh263208
 * @date 2022-09-29 16:49
 * @since ODC_release_3.5.0
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class HostAddress {
    private String host;
    private Integer port;

    public HostAddress(@NonNull com.oceanbase.jdbc.HostAddress hostAddress) {
        this.host = hostAddress.host;
        this.port = hostAddress.port;
    }

    public HostAddress(@NonNull org.mariadb.jdbc.HostAddress hostAddress) {
        this.host = hostAddress.host;
        this.port = hostAddress.port;
    }
}
