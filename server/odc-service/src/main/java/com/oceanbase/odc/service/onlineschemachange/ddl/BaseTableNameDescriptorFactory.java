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
public abstract class BaseTableNameDescriptorFactory implements TableNameDescriptorFactory {

    @Override
    public TableNameDescriptor getTableNameDescriptor(String tableName) {
        DefaultTableNameDescriptor nameDescriptor = new DefaultTableNameDescriptor();
        nameDescriptor.setOriginTableName(tableName);
        nameDescriptor.setOriginTableNameUnwrapped(DdlUtils.getUnwrappedName(tableName));

        String newTableName = DdlUtils.getNewNameWithSuffix(tableName, tablePrefix(), newTableSuffix());
        nameDescriptor.setNewTableName(newTableName);
        nameDescriptor.setNewTableNameUnWrapped(DdlUtils.getUnwrappedName(newTableName));

        String renamedTableName = DdlUtils.getNewNameWithSuffix(tableName, tablePrefix(), renamedTableSuffix());
        nameDescriptor.setRenamedTableName(renamedTableName);
        nameDescriptor.setRenamedTableNameUnWrapped(DdlUtils.getUnwrappedName(renamedTableName));
        return nameDescriptor;
    }

    protected abstract String tablePrefix();

    protected abstract String newTableSuffix();

    protected abstract String renamedTableSuffix();



}
