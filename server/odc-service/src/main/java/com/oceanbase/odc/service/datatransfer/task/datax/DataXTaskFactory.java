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

package com.oceanbase.odc.service.datatransfer.task.datax;

import java.util.List;

import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.task.DataTransferTask;
import com.oceanbase.odc.service.datatransfer.task.TransferTaskFactory;

public class DataXTaskFactory implements TransferTaskFactory {

    public DataXTaskFactory(DataTransferConfig config) {

    }

    @Override
    public List<DataTransferTask> generate() {
        return null;
    }
}
