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
package com.oceanbase.odc.core.migrate.resource.value;

/**
 * {@link UuidGenerator}
 *
 * @author yh263208
 * @date 2022-04-24 11:31
 * @since ODC_release_3.3.1
 */
public class UuidGenerator implements ValueGenerator<String> {

    @Override
    public String generate() {
        return com.oceanbase.odc.common.util.StringUtils.uuid();
    }

}
