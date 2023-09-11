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
package com.oceanbase.odc.service.flow.provider;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.TaskType;
import com.oceanbase.odc.service.datatransfer.file.LocalFileManager;

import lombok.NonNull;

/**
 * {@link BaseExpiredDocumentProvider} for {@code Export Task}
 *
 * @author yh263208
 * @date 2022-03-31 21:05
 * @since ODC_reelase_3.3.0
 */
public class DataTransferExpiredDocumentProvider extends BaseExpiredDocumentProvider {

    private final LocalFileManager fileManager;

    public DataTransferExpiredDocumentProvider(@NonNull LocalFileManager fileManager, @NonNull int fileExpireHours) {
        super(fileExpireHours);
        this.fileManager = fileManager;
    }

    @Override
    public List<File> provide() {
        List<File> returnVal = super.provide();
        return returnVal.stream().filter(file -> {
            String fileName = file.getName();
            return !"ob-loader-dumper.all".equals(fileName) && !"ob-loader-dumper.warn".equals(fileName);
        }).collect(Collectors.toList());
    }

    @Override
    protected File getRootScanDir() throws IOException {
        return fileManager.getWorkingDir(TaskType.EXPORT, null);
    }
}
