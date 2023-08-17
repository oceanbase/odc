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
package com.oceanbase.odc.test.cli;

import lombok.Data;
import lombok.ToString;

/**
 * @author gaoda.xy
 * @date 2023/2/17 12:12
 */
@Data
@ToString(exclude = "password")
public class ConnectionParseResult {
    private String host;
    private Integer port;
    private String cluster;
    private String tenant;
    private String username;
    private String password;
    private String defaultDBName;
}
