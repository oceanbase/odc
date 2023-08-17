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
package com.oceanbase.odc.service.datatransfer.dumper;

import java.io.InputStream;
import java.net.URL;

import com.oceanbase.tools.loaddump.utils.SerializeUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BinaryFile}, refers to {@code Manifest.bin} and {@code CHECKPOINTS.bin}
 *
 * @author yh263208
 * @date 2022-07-01 16:40
 * @since ODC_release_3.4.0
 */
@Getter
@Slf4j
public class BinaryFile<T> {

    private final T target;
    private final URL url;

    private BinaryFile(@NonNull T target, @NonNull URL url) {
        this.url = url;
        this.target = target;
    }

    public static <T> BinaryFile<T> newFile(@NonNull URL url) {
        try (InputStream inputStream = url.openStream()) {
            T value = SerializeUtils.deserializeObjectByKryo(inputStream);
            if (value == null) {
                return null;
            }
            return new BinaryFile<>(value, url);
        } catch (Exception e) {
            return null;
        }
    }

}
