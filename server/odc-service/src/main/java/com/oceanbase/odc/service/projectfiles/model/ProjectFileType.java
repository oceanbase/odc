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

package com.oceanbase.odc.service.projectfiles.model;

/**
 * 项目文件类型
 * 
 * @author keyangs
 * @date 2024/7/31
 * @since 4.3.2
 */
public enum ProjectFileType {
    GIT_REPO(1),
    DIRECTORY(2),
    FILE(3),
    ;

    /**
     * 用以定义ProjectFile类型间排序的优先级，值越小优先级越高
     */
    final int order;

    ProjectFileType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
