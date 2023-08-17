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
package com.oceanbase.odc.core.sql.execute.cache;

import lombok.NonNull;

/**
 * k-v storage object, used to solve the storage layer problem of k-v
 *
 * @author yh263208
 * @date 2021-11-02 21:19
 * @since ODC_release_3.2.2
 */
public interface KeyValueRepository {
    /**
     * put a value
     *
     * @param key key
     * @param value value
     * @return value
     */
    void put(@NonNull String key, @NonNull Object value);

    /**
     * Get value by key
     *
     * @param key key
     * @return value
     */
    Object get(@NonNull String key);

    /**
     * Remove a key
     *
     * @param key key
     * @return remove result
     */
    boolean remove(String key);

}

