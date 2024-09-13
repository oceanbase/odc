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
package com.oceanbase.odc.service.worksheet.constants;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
public class WorksheetConstants {
    /**
     * the limit number of project worksheets search
     */
    public static final int PROJECT_FILES_NAME_LIKE_SEARCH_LIMIT = 10000;

    /**
     * the limit of worksheet name length
     */
    public static final int NAME_LENGTH_LIMIT = 64;
    /**
     * the limit of worksheet path length
     */
    public static final int PATH_LENGTH_LIMIT = 1024;
    /**
     * the limit number of change worksheet in an operation
     */
    public static final int CHANGE_WORKSHEET_NUM_LIMIT = 2000;
    /**
     * the limit number of worksheets in the same level
     */
    public static final int SAME_LEVEL_WORKSHEET_NUM_LIMIT = 1000;

    /**
     * the limit number of worksheets in project
     */
    public static final int PROJECT_WORKSHEET_NUM_LIMIT = 200_000;

    /**
     * the duration of download worksheet in seconds
     */
    public static final long MAX_DURATION_DOWNLOAD_SECONDS = 60 * 60L;

    public static final String ROOT_PATH_NAME = "";
}
