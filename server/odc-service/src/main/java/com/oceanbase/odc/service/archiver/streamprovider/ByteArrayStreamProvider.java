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
package com.oceanbase.odc.service.archiver.streamprovider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.oceanbase.odc.service.archiver.ArchivedDataStreamProvider;

public class ByteArrayStreamProvider implements ArchivedDataStreamProvider {

    private final byte[] bytes;

    public ByteArrayStreamProvider(byte[] bytes) {
        this.bytes = bytes.clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }
}
