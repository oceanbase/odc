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
package com.oceanbase.odc.service.exporter;

import java.io.IOException;

import com.oceanbase.odc.service.exporter.model.ExportProperties;
import com.oceanbase.odc.service.exporter.model.ExportRowDataAppender;
import com.oceanbase.odc.service.exporter.model.ExportedFile;

public interface Exporter {

    ExportedFile archiveFullData(Object data, ExportProperties metaData, String encryptKey) throws Exception;

    ExportedFile archiveFullData(Object data, ExportProperties metaData) throws Exception;

    ExportRowDataAppender buildRowDataAppender(ExportProperties metaData,
            String encryptKey) throws IOException;

    ExportRowDataAppender buildRowDataAppender(ExportProperties metaData)
            throws IOException;
}
