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

import org.junit.Assert;
import org.junit.Test;

public class ExceptionUtilsTest {

    @Test
    public void getRootCauseReason() {
        String rootCauseReason = null;
        try {
            rethrowException(() -> {
                rethrowException(() -> {
                    rethrowException(() -> {
                    });
                    throw new RuntimeException("root cause");
                });
            });
        } catch (Exception e) {
            rootCauseReason = ExceptionUtils.getRootCauseReason(e);
        }
        Assert.assertEquals(
                " \tat com.oceanbase.odc.common.util.ExceptionUtilsTest.rethrowException(ExceptionUtilsTest.java:71)"
                        + "  [wrapped] java.lang.RuntimeException: rethrow: "
                        + " \tat com.oceanbase.odc.common.util.ExceptionUtilsTest.rethrowException(ExceptionUtilsTest.java:69)"
                        + " \tat com.oceanbase.odc.common.util.ExceptionUtilsTest.lambda$null$1(ExceptionUtilsTest.java:31)"
                        + " java.lang.RuntimeException: root cause",
                rootCauseReason);
    }

    @Test
    public void getCauseReason() {
        String causeReason = null;
        try {
            rethrowException(() -> {
                rethrowException(() -> {
                    rethrowException(() -> {
                    });
                    throw new RuntimeException("root cause");
                });
            });
        } catch (Exception e) {
            causeReason = ExceptionUtils.getCauseReason(e);
        }
        Assert.assertEquals(
                " at com.oceanbase.odc.common.util.ExceptionUtilsTest.getCauseReason(ExceptionUtilsTest.java:50) "
                        + "at com.oceanbase.odc.common.util.ExceptionUtilsTest.rethrowException(ExceptionUtilsTest.java:71) "
                        + "java.lang.RuntimeException: rethrow: ",
                causeReason);
    }

    private void rethrowException(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException("rethrow: ", e);
        }
    }
}
