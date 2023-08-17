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
package com.oceanbase.odc.service.cloud;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.objectstorage.cloud.util.CloudObjectStorageUtil;

public class CloudObjectStorageUtilTest {

    @Test
    public void getOriginalFileName_ENUS() {
        String originalFileName = CloudObjectStorageUtil
                .getOriginalFileName("ODC-transfer-2022-10-26/17/AMO_d8N52GtSP7Co1mPsCg%3D%3D/0/test.sql");
        Assert.assertEquals("test.sql", originalFileName);
    }

    @Test
    public void getOriginalFileName_CNZH() {
        String originalFileName = CloudObjectStorageUtil.getOriginalFileName(
                "ODC-transfer-2022-10-26/17/%3D%3D/15/%E4%B8%AD%E6%96%87%E8%84%9A%E6%9C%AC%E5%90%8D%E7%A7%B0.sql");
        Assert.assertEquals("中文脚本名称.sql", originalFileName);
    }

}
