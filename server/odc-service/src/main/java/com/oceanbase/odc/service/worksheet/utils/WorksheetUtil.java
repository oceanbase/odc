/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.worksheet.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import com.oceanbase.odc.service.objectstorage.cloud.model.CloudObjectStorageConstants;
import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;
import com.oceanbase.odc.service.worksheet.domain.Path;

/**
 * @author keyang
 * @date 2024/08/08
 * @since 4.3.2
 */
public class WorksheetUtil {
    /**
     * the bucketName prefix of worksheets which start with /Worksheets/
     */
    private static final String WORKSHEETS_BUCKET_NAME_PREFIX = "PROJECT_WORKSHEETS_";
    /**
     * the objectId prefix of worksheet which start with /Worksheets/
     */
    public static final String WORKSHEETS_OBJECT_ID_PREFIX = "ODC-worksheets-";
    /**
     * the bucketName prefix of worksheets which start with /Repos/RepoName/
     */
    private static final String REPOS_BUCKET_NAME_PREFIX = "PROJECT_REPOS_";
    /**
     * the objectId prefix of worksheet which start with /Repos/RepoName/
     */
    public static final String REPOS_OBJECT_ID_PREFIX = "ODC-repos-";
    /**
     * worksheet download directory
     */
    private static final String WORKSHEET_DOWNLOAD_DIRECTORY =
            CloudObjectStorageConstants.TEMP_DIR + "/worksheet_download";
    private static final String WORKSHEET_DATE_FORMAT = "yyyyMMddHHmmss";

    public static String getBucketNameOfWorkSheets(Long projectId) {
        return WORKSHEETS_BUCKET_NAME_PREFIX + projectId;
    }

    public static String getObjectIdOfWorksheets(Path path) {
        return CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                WORKSHEETS_OBJECT_ID_PREFIX, path.getName());
    }

    public static String getBucketNameOfRepos(Long projectId) {
        return REPOS_BUCKET_NAME_PREFIX + projectId;
    }

    public static String getObjectIdOfRepos(Path path) {
        return CloudObjectStorageUtil.generateObjectName(null, UUID.randomUUID().toString(),
                REPOS_OBJECT_ID_PREFIX, path.getName());
    }

    public static String getWorksheetDownloadDirectory() {
        int hashCode = UUID.randomUUID().hashCode();
        SimpleDateFormat format = new SimpleDateFormat(WORKSHEET_DATE_FORMAT);
        format.setTimeZone(TimeZone.getDefault());
        return String.format("%s/%s/", WORKSHEET_DOWNLOAD_DIRECTORY,
                format.format(new Date()) + hashCode);
    }

    public static String getWorksheetDownloadZipPath(String directory, String fileName) {
        return String.format("%s/%s.zip",
                directory,
                fileName);
    }
}
