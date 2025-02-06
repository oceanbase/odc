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

import java.io.File;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.service.archiver.streamprovider.FileStreamProvider;
import com.oceanbase.odc.service.archiver.streamprovider.RemoteUriStreamProvider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArchivedFile {

    /**
     * Local path or download url
     */
    private String uri;

    private String fileName;

    /**
     * AES secret， null means decrypt data
     */
    @JsonIgnore
    private String secret;

    private ArchivedDataStreamProvider provider;

    public static ArchivedFile fromRemoteUrl(String remoteUrl, @Nullable String secret) {
        ArchivedFile archivedFile = new ArchivedFile();
        archivedFile.setUri(remoteUrl);
        ArchivedDataStreamProvider provider = new RemoteUriStreamProvider(remoteUrl);
        archivedFile.setProvider(provider);
        archivedFile.setSecret(secret);
        return archivedFile;
    }

    public static ArchivedFile fromFile(File file, @Nullable String secret) {
        ArchivedFile archivedFile = new ArchivedFile();
        archivedFile.setUri(file.getAbsoluteFile() + File.separator + file.getName());
        ArchivedDataStreamProvider provider = new FileStreamProvider(file);
        archivedFile.setProvider(provider);
        archivedFile.setSecret(secret);
        return archivedFile;
    }
}
