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
package com.oceanbase.odc.service.projectfiles.constants;

/**
 * @author keyang
 * @date 2024/08/05
 * @since 4.3.2
 */
public class ProjectFilesConstant {
    /**
     * 项目文件搜索数量限制
     */
    public static final int PROJECT_FILES_NAME_LIKE_SEARCH_LIMIT = 100;

    /**
     * 文件名长度限制
     */
    public static final int NAME_LENGTH_LIMIT = 64;
    /**
     * 每次变更数量限制
     */
    public static final int CHANGE_FILE_NUM_LIMIT = 2000;
    /**
     * 同一级目录文件数量限制
     */
    public static final int LEVEL_FILE_NUM_LIMIT = 100;
}
