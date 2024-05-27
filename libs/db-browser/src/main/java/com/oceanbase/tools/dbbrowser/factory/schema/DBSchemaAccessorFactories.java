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
package com.oceanbase.tools.dbbrowser.factory.schema;

import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactories;
import com.oceanbase.tools.dbbrowser.factory.DBBrowserFactory;
import com.oceanbase.tools.dbbrowser.schema.DBSchemaAccessor;

public class DBSchemaAccessorFactories implements DBBrowserFactories<DBSchemaAccessor> {

    @Override
    public <Factory extends DBBrowserFactory<DBSchemaAccessor>> Factory forMySQL() {
        return null;
    }

    @Override
    public <Factory extends DBBrowserFactory<DBSchemaAccessor>> Factory forOracle() {
        return null;
    }

    @Override
    public OBMySQLDBSchemaAccessorFactory forOBMySQL() {
        return new OBMySQLDBSchemaAccessorFactory();
    }

    @Override
    public OBOracleDBSchemaAccessorFactory forOBOracle() {
        return new OBOracleDBSchemaAccessorFactory();
    }

    @Override
    public <Factory extends DBBrowserFactory<DBSchemaAccessor>> Factory forDoris() {
        return null;
    }

}
