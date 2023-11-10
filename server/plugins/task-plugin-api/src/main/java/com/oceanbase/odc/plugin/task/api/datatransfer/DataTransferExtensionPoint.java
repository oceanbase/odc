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

package com.oceanbase.odc.plugin.task.api.datatransfer;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.UploadFileResult;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

import lombok.NonNull;

/**
 * @author liuyizhuo.lyz
 * @date 2023-09-15
 */
public interface DataTransferExtensionPoint extends ExtensionPoint {

    DataTransferJob generate(@NonNull DataTransferConfig config, @NonNull File workingDir, @NonNull File logDir,
            @NonNull List<URL> inputs) throws Exception;

    Set<ObjectType> getSupportedObjectTypes(ConnectionInfo connectionInfo) throws SQLException;

    Set<DataTransferFormat> getSupportedTransferFormats();

    UploadFileResult getImportFileInfo(@NonNull URL url) throws URISyntaxException;

}
