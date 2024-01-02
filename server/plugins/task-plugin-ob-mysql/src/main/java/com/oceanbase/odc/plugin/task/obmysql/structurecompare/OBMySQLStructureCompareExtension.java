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
package com.oceanbase.odc.plugin.task.obmysql.structurecompare;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.pf4j.Extension;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.schema.obmysql.utils.DBAccessorUtil;
import com.oceanbase.odc.plugin.task.api.structurecompare.StructureComparator;
import com.oceanbase.odc.plugin.task.api.structurecompare.StructureCompareExtensionPoint;
import com.oceanbase.odc.plugin.task.api.structurecompare.model.StructureCompareConfig;

/**
 * @author jingtian
 * @date 2024/1/2
 * @since ODC_release_4.2.4
 */
@Extension
public class OBMySQLStructureCompareExtension implements StructureCompareExtensionPoint {
    @Override
    public StructureComparator getStructureComparator(StructureCompareConfig config) throws SQLException {
        checkConfig(config);
        DataSource srcDataSource = config.getSourceDataSource();
        DataSource tgtDataSource = config.getTargetDataSource();
        return new OdcStructureComparator(config.getSourceSchemaName(), config.getTargetSchemaName(),
                DBAccessorUtil.getSchemaAccessor(srcDataSource.getConnection()),
                DBAccessorUtil.getSchemaAccessor(tgtDataSource.getConnection()),
                DBAccessorUtil.getTableEditor(tgtDataSource.getConnection()), config.getTargetDialectType());
    }

    private void checkConfig(StructureCompareConfig config) {
        if (config.getSourceDialectType() != DialectType.OB_MYSQL
                || config.getTargetDialectType() != DialectType.OB_MYSQL) {
            throw new IllegalArgumentException("Source and target dialect type must be OB_MYSQL");
        }
    }
}
