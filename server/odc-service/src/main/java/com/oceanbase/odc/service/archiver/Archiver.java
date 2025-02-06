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

import java.util.Properties;

import javax.annotation.Nullable;

public interface Archiver {

    /**
     * Stores an object as a local encrypt file
     * 
     * @param data
     * @param metaData
     * @param encryptKey
     * @return
     * @throws Exception
     */
    ArchivedFile archiveFullDataToLocal(Object data, @Nullable Properties metaData, String encryptKey)
            throws Exception;

    /**
     * Stores an object as a remote encrypt file
     * 
     * @param data
     * @param metaData
     * @param encryptKey
     * @return
     * @throws Exception
     */
    ArchivedFile archiveFullDataToCloudObject(Object data, @Nullable Properties metaData, String encryptKey)
            throws Exception;

    /**
     * Stores an object as a local decrypt file
     *
     * @param data
     * @param metaData
     * @return
     * @throws Exception
     */
    ArchivedFile archiveFullDataToLocal(Object data, @Nullable Properties metaData) throws Exception;


    /**
     * Stores an object as a remote decrypt file
     *
     * @param data
     * @param metaData
     * @return
     * @throws Exception
     */
    ArchivedFile archiveFullDataToCloudObject(Object data, @Nullable Properties metaData) throws Exception;

}
