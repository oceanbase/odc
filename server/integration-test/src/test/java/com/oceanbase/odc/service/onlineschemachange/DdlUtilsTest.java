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
package com.oceanbase.odc.service.onlineschemachange;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.onlineschemachange.ddl.DdlConstants;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;

/**
 * @author yaobin
 * @date 2023-06-15
 * @since 4.2.0
 */
public class DdlUtilsTest {

    @Test
    public void test_table_name_with_quote_no_schema() {
        String tableName = "\"t\"";

        String newName = DdlUtils.getNewNameWithSuffix(tableName, DdlConstants.RENAMED_TABLE_NAME_SUFFIX);

        Assert.assertEquals("\"_t" + DdlConstants.RENAMED_TABLE_NAME_SUFFIX + "\"", newName);

    }

    @Test
    public void test_table_name_with_accent_no_schema() {
        String tableName = "`t`";

        String newName = DdlUtils.getNewNameWithSuffix(tableName, DdlConstants.RENAMED_TABLE_NAME_SUFFIX);

        Assert.assertEquals("`_t" + DdlConstants.RENAMED_TABLE_NAME_SUFFIX + "`", newName);

    }

}
