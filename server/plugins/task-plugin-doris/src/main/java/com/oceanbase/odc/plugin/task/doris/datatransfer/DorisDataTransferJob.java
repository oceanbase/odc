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
package com.oceanbase.odc.plugin.task.doris.datatransfer;

import java.io.File;
import java.net.URL;
import java.util.List;

import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.doris.datatransfer.job.DorisTransferJobFactory;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.MySQLDataTransferJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.factory.BaseTransferJobFactory;

import lombok.NonNull;

/**
 * @author liuyizhuo.lyz
 * @date 2024/3/28
 */
public class DorisDataTransferJob extends MySQLDataTransferJob {

    public DorisDataTransferJob(
            @NonNull DataTransferConfig config,
            @NonNull File workingDir, @NonNull File logDir,
            @NonNull List<URL> inputs) {
        super(config, workingDir, logDir, inputs);
    }

    @Override
    protected BaseTransferJobFactory getJobFactory() {
        return new DorisTransferJobFactory(baseConfig, workingDir, logDir, inputs);
    }

}
