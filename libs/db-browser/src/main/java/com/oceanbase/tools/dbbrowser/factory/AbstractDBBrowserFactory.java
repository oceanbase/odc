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
package com.oceanbase.tools.dbbrowser.factory;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public abstract class AbstractDBBrowserFactory<T> implements DBBrowserFactory<T> {

    private String type;

    @Override
    public T create() {
        switch (type) {
            case ORACLE:
                return buildForOracle();
            case MYSQL:
                return buildForMysql();
            case DORIS:
                return buildForDoris();
            case OB_ORACLE:
                return buildForOBOracle();
            case OB_MYSQL:
                return buildForOBMysql();
            default:
                throw new IllegalStateException("Not supported for the type, " + type);
        }
    }

    public abstract T buildForDoris();

    public abstract T buildForMysql();

    public abstract T buildForOBMysql();

    public abstract T buildForOBOracle();

    public abstract T buildForOracle();

}
