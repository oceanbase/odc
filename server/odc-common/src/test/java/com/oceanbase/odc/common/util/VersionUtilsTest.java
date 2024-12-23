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

public class VersionUtilsTest {

    @Test
    public void compareVersions() {
        Assert.assertTrue(VersionUtils.compareVersions("1.0.1", "1.1.2") < 0);
        Assert.assertTrue(VersionUtils.compareVersions("1.0.1", "1.10") < 0);
        Assert.assertTrue(VersionUtils.compareVersions("1.1.2", "1.0.1") > 0);
        Assert.assertTrue(VersionUtils.compareVersions("1.1.2", "1.2.0") < 0);
        Assert.assertEquals(0, VersionUtils.compareVersions("1.3.0", "1.3"));
    }

    @Test
    public void isGreaterThanOrEqualsTo() {
        Assert.assertTrue(VersionUtils.isGreaterThanOrEqualsTo("1.1.2", "1.1.1"));
        Assert.assertTrue(VersionUtils.isGreaterThanOrEqualsTo("1.1.1", "1.1.1"));
        Assert.assertFalse(VersionUtils.isGreaterThanOrEqualsTo("1.1.0", "1.1.1"));
    }

    @Test
    public void isGreaterThan_Length3() {
        Assert.assertTrue(VersionUtils.isGreaterThan("1.1.2", "1.1.1"));
        Assert.assertFalse(VersionUtils.isGreaterThan("1.1.1", "1.1.1"));
        Assert.assertFalse(VersionUtils.isGreaterThan("1.1.0", "1.1.1"));
    }

    @Test
    public void isGreaterThan_Length4() {
        Assert.assertTrue(VersionUtils.isGreaterThan("1.1.1.1", "1.1.1.0"));
        Assert.assertFalse(VersionUtils.isGreaterThan("1.1.1.0", "1.1.1.0"));
        Assert.assertFalse(VersionUtils.isGreaterThan("1.1.0.0", "1.1.1.0"));
    }

    @Test
    public void isGreaterThan0_Length() {
        Assert.assertTrue(VersionUtils.isGreaterThan0("1.1.2"));
        Assert.assertFalse(VersionUtils.isGreaterThan0("-1"));
        Assert.assertFalse(VersionUtils.isGreaterThan0("0"));
    }

    @Test
    public void isGreaterThan_Length4WithLength3() {
        Assert.assertTrue(VersionUtils.isGreaterThan("1.1.1.1", "1.1.1"));
        Assert.assertFalse(VersionUtils.isGreaterThan("1.1.1.0", "1.1.1"));
    }

    @Test
    public void isGreaterThan_Length3WithLength4() {
        Assert.assertTrue(VersionUtils.isGreaterThan("1.1.1", "1.1.0.1"));
        Assert.assertFalse(VersionUtils.isGreaterThan("1.1.1", "1.1.1.0"));
        Assert.assertFalse(VersionUtils.isGreaterThan("1.1.1", "1.1.1.1"));
    }

    @Test
    public void isGreaterThan_OB3120WithOB2277_ReturnTrue() {
        Assert.assertTrue(VersionUtils.isGreaterThan("3.1.2.1", "2.2.77"));
    }
}
