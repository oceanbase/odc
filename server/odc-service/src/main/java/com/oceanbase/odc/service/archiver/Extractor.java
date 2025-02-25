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
package com.oceanbase.odc.service.archiver;

import java.io.Closeable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.oceanbase.odc.service.archiver.model.ExportRowDataReader;
import com.oceanbase.odc.service.archiver.model.ExportedData;
import com.oceanbase.odc.service.archiver.model.ExportedFile;

public interface Extractor<R> extends Closeable {

    boolean checkSignature();

    ExportedFile getExportedFile();

    <D> ExportedData<D> extractFullData(TypeReference<ExportedData<D>> typeReference)
            throws Exception;

    ExportRowDataReader<R> getRowDataReader() throws Exception;
}
