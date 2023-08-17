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
package com.oceanbase.odc.service.resultset;

import java.io.File;

import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.flow.provider.BaseExpiredDocumentProvider;

import lombok.NonNull;

public class ResultSetExportExpiredFileProvider extends BaseExpiredDocumentProvider {

    public ResultSetExportExpiredFileProvider(@NonNull int fileExpireHours) {
        super(fileExpireHours);
    }

    @Override
    protected File getRootScanDir() {
        return new File(FileManager.generatePath(FileBucket.RESULT_SET));
    }

}
