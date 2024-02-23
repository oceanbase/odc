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

package com.oceanbase.odc.service.datatransfer;

import org.junit.Ignore;

import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

/**
 * Due to the current single test on GitHub being executed by ob-farm, it is temporarily not
 * possible to obtain the lfs file. So this unit test will be temporarily skipped and manually
 * executed by the developer.
 */
@Ignore("run it manually")
public class MySQLTransferServiceTest extends BaseTransferServiceTest {

    @Override
    protected ConnectionConfig getConnectionConfig() {
        return TestConnectionUtil.getTestConnectionConfig(ConnectType.from(DialectType.MYSQL));
    }

    @Override
    protected String getTableName() {
        return "loader_dumper_test";
    }

}
