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
package com.oceanbase.odc.core.migrate;

import javax.sql.DataSource;

/**
 * @author yiminpeng
 * @date 2024/07/26
 * @version : BeforeMigrate.java, v 1.0
 */
public interface BeforeMigrate {

    void executeDeleteBeforeCheck(DataSource dataSource, String initVersion);

}
