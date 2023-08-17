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
package com.oceanbase.odc.service.flow.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.core.shared.exception.InternalServerError;

import lombok.NonNull;

/**
 * {@link FileBasedDataResult}
 *
 * @author yh263208
 * @date 2022-03-28 22:07
 * @since ODC_release_3.3.0
 */
public class FileBasedDataResult implements BinaryDataResult {

    private final File target;

    public FileBasedDataResult(@NonNull File file) {
        Verify.verify(file.exists(), "Target file does not exist " + file.getAbsolutePath());
        Verify.verify(file.isFile(), "Target is not a file " + file.getAbsolutePath());
        this.target = file;
    }

    @Override
    public String getName() {
        return target.getName();
    }

    @Override
    public InputStream getInputStream() {
        try {
            return new FileInputStream(target);
        } catch (Exception e) {
            throw new InternalServerError(e.getMessage());
        }
    }
}
