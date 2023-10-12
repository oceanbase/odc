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

package com.oceanbase.odc.service.onlineschemachange.ddl;

/**
 * @author yaobin
 * @date 2023-08-31
 * @since 4.2.0
 */
public class OBMySqlTableNameDescriptorFactory extends BaseTableNameDescriptorFactory {

    @Override
    protected String tablePrefix() {
        return DdlConstants.OSC_TABLE_NAME_PREFIX;
    }

    @Override
    protected String newTableSuffix() {
        return DdlConstants.NEW_TABLE_NAME_SUFFIX;
    }

    @Override
    protected String renamedTableSuffix() {
        return DdlConstants.RENAMED_TABLE_NAME_SUFFIX;
    }
}
