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
package com.oceanbase.odc.service.datatransfer;

import java.io.File;
import java.io.IOException;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTaskContext;
import com.oceanbase.odc.service.flow.task.model.DataTransferTaskResult;
import com.oceanbase.tools.loaddump.common.model.BaseParameter;

public interface DataTransferAdapter {

    Long getMaxDumpSizeBytes();

    File preHandleWorkDir(DataTransferConfig transferConfig,
            String bucket, File workDir) throws IOException;

    void afterHandle(BaseParameter parameter, DataTransferTaskContext context,
            DataTransferTaskResult result, File exportFile) throws IOException;

}
