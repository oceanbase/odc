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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.resource;

import java.util.List;

public interface ResourceFinder<Resource> {

    /**
     * List the matched schema file resources.
     *
     * @return List<T>
     * @throws Exception
     */
    List<Resource> listSchemaResources() throws Exception;


    /**
     * List the matched record file resources.
     *
     * @return List<T>
     * @throws Exception
     */
    List<Resource> listRecordResources() throws Exception;

}
