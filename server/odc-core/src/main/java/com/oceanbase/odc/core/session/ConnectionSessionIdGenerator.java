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
package com.oceanbase.odc.core.session;

/**
 * {@link ConnectionSession} id generator, used to generate unique id
 *
 * @author yh263208
 * @date 2021-11-15 16:04
 * @since ODC_release_3.2.2
 */
public interface ConnectionSessionIdGenerator {
    /**
     * Generate method, used to generate an unique id
     *
     * @return Generated Id
     */
    String generateId();

}

