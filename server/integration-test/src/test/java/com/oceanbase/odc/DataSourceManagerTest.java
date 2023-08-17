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
package com.oceanbase.odc;

import lombok.extern.slf4j.Slf4j;

/**
 * 2020-12-18 移除 testGetOCJConnection，此方法会抛 log4j Logger ClassNotFound 异常
 */
@Slf4j
public class DataSourceManagerTest {

    public void test() {
        log.info("TODO: add UT for DataSourceManager");
    }
}
