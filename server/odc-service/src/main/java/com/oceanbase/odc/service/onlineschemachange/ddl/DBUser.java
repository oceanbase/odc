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

package com.oceanbase.odc.service.onlineschemachange.ddl;

import com.oceanbase.tools.dbbrowser.model.DBObject;
import com.oceanbase.tools.dbbrowser.model.DBObjectType;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yaobin
 * @date 2023-10-13
 * @since 4.2.3
 */
@EqualsAndHashCode
@Data
public class DBUser implements DBObject {

    private String name;

    private DBAccountLockType accountLocked;

    private String host;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public DBObjectType type() {
        return DBObjectType.USER;
    }

    public String getNameWithHost() {
        return name + (host == null ? "" : "@'" + host + "'");
    }
}
