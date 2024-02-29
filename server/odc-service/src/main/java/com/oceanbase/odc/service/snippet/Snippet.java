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
package com.oceanbase.odc.service.snippet;

import java.sql.Timestamp;

import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class Snippet {

    private Long id;
    private Long userId;
    private String prefix;
    @Size(max = 65535, message = "Snippet body is out of range [0, 65535]")
    private String body;
    private String description;
    private String type;
    private Timestamp createTime;
    private Timestamp modifyTime;

}
