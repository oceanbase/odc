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

package com.oceanbase.odc.plugin.task.oboracle.datatransfer;

import java.sql.SQLException;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.pf4j.Extension;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.OBMySQLDataTransferExtension;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

@Extension
public class OBOracleDataTransferExtension extends OBMySQLDataTransferExtension {

    @Override
    public Set<ObjectType> getSupportedObjectTypes(ConnectionInfo connectionInfo) throws SQLException {
        Set<ObjectType> types = SetUtils.hashSet(
                ObjectType.SEQUENCE,
                ObjectType.TRIGGER,
                ObjectType.PACKAGE,
                ObjectType.PACKAGE_BODY,
                ObjectType.SYNONYM,
                ObjectType.PUBLIC_SYNONYM,
                ObjectType.TYPE);
        return SetUtils.union(types, super.getSupportedObjectTypes(connectionInfo));
    }

}
