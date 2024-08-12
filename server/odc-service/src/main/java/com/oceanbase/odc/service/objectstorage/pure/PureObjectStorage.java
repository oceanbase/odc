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
package com.oceanbase.odc.service.objectstorage.pure;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.oceanbase.odc.service.objectstorage.lifecycle.Lifecycle;

/**
 * only for the logic of object storage ,not include the processing of metadata.
 * </p>
 * In addition, the bucketName uses the default value and cannot be customized
 * 
 * @author keyang
 * @date 2024/08/09
 * @since 4.3.2
 */
public interface PureObjectStorage {
    URL getDownloadUrl(@NotBlank String objectName, Long expirationSeconds);

    URL getUploadUrl(@NotBlank String objectName);

    void putObject(@NotBlank String objectName, @NotNull File file, Lifecycle lifecycle) throws IOException;

    byte[] readContent(@NotBlank String objectName) throws IOException;

    void downloadToFile(@NotBlank String objectName, @NotNull File targetFile) throws IOException;

    List<String> deleteObjects(@NotEmpty List<String> objectNames) throws IOException;
}
