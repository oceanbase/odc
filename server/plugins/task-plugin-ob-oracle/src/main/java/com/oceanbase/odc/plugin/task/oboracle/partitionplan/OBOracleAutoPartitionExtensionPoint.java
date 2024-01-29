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
package com.oceanbase.odc.plugin.task.oboracle.partitionplan;

import org.pf4j.Extension;

import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.obmysql.partitionplan.OBMySQLAutoPartitionExtensionPoint;

import lombok.NonNull;

/**
 * {@link OBOracleAutoPartitionExtensionPoint}
 *
 * @author yh263208
 * @date 2024-01-26 13:38
 * @since ODC_release_4.2.4
 * @see OBMySQLAutoPartitionExtensionPoint
 */
@Extension
public class OBOracleAutoPartitionExtensionPoint extends OBMySQLAutoPartitionExtensionPoint {

    @Override
    public String unquoteIdentifier(@NonNull String identifier) {
        return ConnectionSessionUtil.getUserOrSchemaString(identifier, DialectType.OB_ORACLE);
    }

}
