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
package com.oceanbase.odc.service.schedule.archiverist.model;

import com.oceanbase.odc.service.archiver.model.Encryptable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveDatabase implements Encryptable {
    ArchiveDataSource archiveDataSource;
    String schema;

    public static ArchiveDatabase of(ArchiveDataSource archiveDataSource, String schema) {
        return new ArchiveDatabase(archiveDataSource, schema);
    }

    @Override
    public void encrypt(String encryptKey) {
        archiveDataSource.encrypt(encryptKey);
    }

    @Override
    public void decrypt(String encryptKey) {
        archiveDataSource.decrypt(encryptKey);
    }
}
