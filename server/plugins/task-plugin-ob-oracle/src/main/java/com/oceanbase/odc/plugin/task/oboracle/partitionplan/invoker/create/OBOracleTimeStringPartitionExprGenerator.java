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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan.invoker.create;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.invoker.create.OBMysqlTimeStringPartitionExprGenerator;

public class OBOracleTimeStringPartitionExprGenerator extends OBMysqlTimeStringPartitionExprGenerator {

    @Override
    protected String unquoteIdentifier(String identifier) {
        return ConnectionSessionUtil.getUserOrSchemaString(identifier, DialectType.OB_ORACLE);
    }

    @Override
    protected String unquoteValue(String value) {
        return StringUtils.unquoteOracleValue(value);
    }

}
