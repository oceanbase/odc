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
package com.oceanbase.odc.plugin.task.oracle.datatransfer;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;

import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferJob;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.MySQLDataTransferExtension;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.NonNull;

/**
 * @author liuyizhuo.lyz
 * @date 2024/1/29
 */
public class OracleDataTransferExtension extends MySQLDataTransferExtension {

    @Override
    public DataTransferJob generate(@NonNull DataTransferConfig config, @NonNull File workingDir, @NonNull File logDir,
            @NonNull List<URL> inputs) throws Exception {
        return new OracleDataTransferJob(config, workingDir, logDir, inputs);
    }

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
