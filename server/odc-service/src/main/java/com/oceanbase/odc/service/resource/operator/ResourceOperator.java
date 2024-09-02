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
package com.oceanbase.odc.service.resource.operator;

import java.util.List;
import java.util.Optional;

/**
 * {@link ResourceOperator}
 *
 * @author yh263208
 * @date 2024-09-02 16:13
 * @since ODC_release_4.3.2
 */
public interface ResourceOperator<T, ID> {

    T create(T config) throws Exception;

    ID getKey(T config);

    Optional<T> query(ID key) throws Exception;

    List<T> list() throws Exception;

    void destroy(ID key) throws Exception;

}
