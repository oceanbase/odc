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

public interface DBBrowserFactories<T> {

    String MYSQL = "MYSQL";
    String OB_MYSQL = "OB_MYSQL";
    String OB_ORACLE = "OB_ORACLE";
    String ORACLE = "ORACLE";
    String DORIS = "DORIS";

    default DBBrowserFactory<T> type(String type) {
        switch (type) {
            case ORACLE:
                return oracle();
            case MYSQL:
                return mysql();
            case DORIS:
                return doris();
            case OB_ORACLE:
                return oboracle();
            case OB_MYSQL:
                return obmysql();
            default:
                throw new IllegalStateException("Not supported for the type, " + type);
        }
    }

    DBBrowserFactory<T> doris();

    DBBrowserFactory<T> mysql();

    DBBrowserFactory<T> obmysql();

    DBBrowserFactory<T> oboracle();

    DBBrowserFactory<T> oracle();

}
