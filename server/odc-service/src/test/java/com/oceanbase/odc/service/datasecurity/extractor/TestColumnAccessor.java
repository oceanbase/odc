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
package com.oceanbase.odc.service.datasecurity.extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oceanbase.odc.service.datasecurity.accessor.ColumnAccessor;

/**
 * @author gaoda.xy
 * @date 2023/6/9 11:32
 */
public class TestColumnAccessor implements ColumnAccessor {

    public static final String OB_MYSQL_DATABASE_1 = "gaoda_test_data";
    public static final String OB_ORACLE_DATABASE_1 = "GAODA";
    private static final String TABLE_1 = "test_data_masking_1";
    private static final String TABLE_2 = "test_data_masking_2";
    private static final String TABLE_3 = "test_data_masking_3";

    @Override
    public List<String> getColumns(String databaseName, String tableName) {
        if (OB_MYSQL_DATABASE_1.equals(databaseName)) {
            switch (tableName) {
                case TABLE_1:
                    return Arrays.asList("id", "name", "birthday", "description");
                case TABLE_2:
                    return Arrays.asList("id", "name", "salary");
                case TABLE_3:
                    return Arrays.asList("id", "name", "level");
                default:
                    return new ArrayList<>();
            }
        }
        if (OB_ORACLE_DATABASE_1.equals(databaseName)) {
            switch (tableName.toLowerCase()) {
                case TABLE_1:
                    return Arrays.asList("ID", "NAME", "BIRTHDAY", "DESCRIPTION");
                case TABLE_2:
                    return Arrays.asList("ID", "NAME", "SALARY");
                case TABLE_3:
                    return Arrays.asList("ID", "NAME", "LEVEL");
                default:
                    return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

}
