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
package com.oceanbase.odc.service.exporter.model;

public final class ExportConstants {

    public static final String HMAC_ALGORITHM = "HmacSHA256";

    public static final String ODC_VERSION = "odcVersion";
    public static final String CREATE_TIME = "createTime";
    public static final String ARCHIVE_TYPE = "archiveType";

    public static final String SCHEDULE_ARCHIVE_TYPE = "scheduleArchive";

    /**
     * filePath, include filename and filetype
     */
    public static final String FILE_NAME = "fileName";
    public static final String FILE_PATH = "filePath";

    public static final String FILE_ZIP_SUFFER = ".zip";
}
