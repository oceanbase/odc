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
package com.oceanbase.odc.common.util;

import java.time.OffsetDateTime;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import lombok.Data;

public class CSVUtilsTest {

    @Test
    public void buildCSVFormatData() {
        A a = new A();
        a.c1 = 1L;
        a.c2 = "ab ";
        a.c3 = AuditEventType.EXPORT;
        a.c4 = OffsetDateTime.parse("2022-03-31T23:02:43+08:00");

        String csv = CSVUtils.buildCSVFormatData(Arrays.asList(a), A.class);

        Assert.assertEquals("c1,c2,c3,c4\n"
                + "1,\"ab \",EXPORT,2022-03-31T23:02:43+08:00\n", csv);
    }

    @Data
    public class A {
        private Long c1;
        private String c2;
        private AuditEventType c3;
        private OffsetDateTime c4;
    }

    enum AuditEventType {
        EXPORT
    }
}
