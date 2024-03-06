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
package com.oceanbase.odc.plugin.connect.oboracle;

import java.util.ArrayList;
import java.util.List;

import org.pf4j.Extension;

import com.oceanbase.odc.core.datasource.ConnectionInitializer;
import com.oceanbase.odc.plugin.connect.obmysql.OBMySQLConnectionExtension;
import com.oceanbase.odc.plugin.connect.obmysql.initializer.EnableTraceInitializer;
import com.oceanbase.odc.plugin.connect.oboracle.initializer.OracleDBMSOutputInitializer;

/**
 * @author yaobin
 * @date 2023-04-14
 * @since 4.2.0
 */
@Extension
public class OBOracleConnectionExtension extends OBMySQLConnectionExtension {

    @Override
    public List<ConnectionInitializer> getConnectionInitializers() {
        List<ConnectionInitializer> initializers = new ArrayList<>();
        initializers.add(new EnableTraceInitializer());
        initializers.add(new OracleDBMSOutputInitializer());
        return initializers;
    }

}
