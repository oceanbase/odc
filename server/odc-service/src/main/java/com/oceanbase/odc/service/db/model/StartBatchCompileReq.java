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
package com.oceanbase.odc.service.db.model;

import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;

/**
 * @author wenniu.ly
 * @date 2022/6/10
 *
 *       <pre>
 * Start a batch compilation by two ways
 * 1. Use [scope] to specify whether you want to compile all objects or invalid objects
 *    - [type] maybe optional to specify what kind of PL object you want to compile
 *    - If [type] is not specified, that means compile all PL object type
 *
 * 2. Use [PLIdentities] to specify all the PL-objects' type and name
 *
 * Attention: [scope] and [PLIdentities] can not be used simultaneously
 *       </pre>
 */

@Data
public class StartBatchCompileReq {
    private String scope;
    private DBObjectType objectType;
}
