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
package com.oceanbase.odc.service.archiver.model;

import java.io.File;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oceanbase.odc.core.shared.Verify;
import com.oceanbase.odc.service.archiver.streamprovider.ArchivedDataStreamProvider;
import com.oceanbase.odc.service.archiver.streamprovider.FileStreamProvider;
import com.oceanbase.odc.service.archiver.streamprovider.RemoteUriStreamProvider;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString.Exclude;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArchivedFile {

    public static final String LOCAL_FILE = "local";
    public static final String REMOTE_FILE = "remote";

    /**
     * Local, Remote
     */
    private String fileType;

    /**
     * Local path or download url
     */
    private String uri;

    private String fileName;

    /**
     * AES secret， null means decrypt data
     */
    @Exclude
    private String secret;

    private boolean checkConfigJsonSignature;

    @JsonIgnore
    private transient ArchivedDataStreamProvider provider;

    public static ArchivedFile fromRemoteUrl(String remoteUrl, @Nullable String secret) {
        ArchivedFile archivedFile = new ArchivedFile();
        archivedFile.setUri(remoteUrl);
        archivedFile.setCheckConfigJsonSignature(secret != null);
        archivedFile.setFileType(REMOTE_FILE);
        ArchivedDataStreamProvider provider = new RemoteUriStreamProvider(remoteUrl);
        archivedFile.setProvider(provider);
        archivedFile.setSecret(secret);
        return archivedFile;
    }

    public static ArchivedFile fromFile(File file, @Nullable String secret) {
        ArchivedFile archivedFile = new ArchivedFile();
        archivedFile.setFileType(LOCAL_FILE);
        archivedFile.setCheckConfigJsonSignature(secret != null);
        archivedFile.setUri(file.getPath());
        ArchivedDataStreamProvider provider = new FileStreamProvider(file);
        archivedFile.setProvider(provider);
        archivedFile.setSecret(secret);
        return archivedFile;
    }

    public File toFile() {
        Verify.equals(fileType, LOCAL_FILE, "file type not supported");
        return new File(uri);
    }
}
