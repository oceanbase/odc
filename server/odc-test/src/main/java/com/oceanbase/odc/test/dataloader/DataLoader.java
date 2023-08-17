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
package com.oceanbase.odc.test.dataloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface DataLoader {

    default <T> T fromFile(String fileName, Class<T> classType) {
        File file = new File(fileName);
        return fromFile(file, classType);
    }

    default <T> T fromFile(File file, Class<T> classType) {
        try (InputStream input = new FileInputStream(file)) {
            return fromInputStream(input, classType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    <T> T fromInputStream(InputStream input, Class<T> classType);
}
