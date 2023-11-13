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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LocalResource implements Resource {

    private final Path path;

    private final String objectName;

    private final String objectType;

    public LocalResource(Path path, String objectName, String objectType) {
        this.path = path;
        this.objectName = objectName;
        this.objectType = objectType;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new FileInputStream(path.toFile());
    }
}
