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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class SSRFCheckerTest {
    private final List<String> hostWhiteList = Arrays.asList("127.0.0.1", "1.1.1.1", "aliyun.com");

    private final List<String> urlWhiteList = Arrays.asList("http://127.0.0.1/api");

    private final List<String> urlBlackList = Arrays.asList("127.0.0.1");

    @Test
    public void checkHostWhiteList_IpNotInWhiteList_ReturnFalse() {
        assertFalse(SSRFChecker.checkHostInWhiteList("2.2.2.2", hostWhiteList));
    }

    @Test
    public void checkHostWhiteList_IpInWhiteList_ReturnTrue() {
        assertTrue(SSRFChecker.checkHostInWhiteList("127.0.0.1", hostWhiteList));
    }

    @Test
    public void checkHostWhiteList_WhiteListIsNull_ReturnTrue() {
        assertTrue(SSRFChecker.checkHostInWhiteList("127.0.0.1", null));
    }

    @Test
    public void checkHostWhiteList_WhiteListIsEmpty_ReturnTrue() {
        assertTrue(SSRFChecker.checkHostInWhiteList("127.0.0.1", Lists.newArrayList()));
    }

    @Test
    public void checkHostWhiteList_ExactDomain_ReturnTrue() {
        assertTrue(SSRFChecker.checkHostInWhiteList("aliyun.com", hostWhiteList));
    }

    @Test
    public void checkHostWhiteList_FuzzyDomain_ReturnTrue() {
        assertTrue(SSRFChecker.checkHostInWhiteList("a.aliyun.com", hostWhiteList));
    }

    @Test
    public void checkHostWhiteList_DomainNotInWhiteList_ReturnFalse() {
        assertFalse(SSRFChecker.checkHostInWhiteList("alipay.com", hostWhiteList));
    }

    @Test
    public void checkUrlWhiteList_UrlInWhiteList_ReturnTrue() {
        assertTrue(SSRFChecker.checkUrlInWhiteList("http://127.0.0.1/api", urlWhiteList));
    }

    @Test
    public void checkUrlWhiteList_UrlInWhiteList_ReturnTrue_2() {
        assertTrue(SSRFChecker.checkUrlInWhiteList("http://127.0.0.1/api/a/b/c", urlWhiteList));
    }

    @Test
    public void checkUrlWhiteList_UrlNotInWhiteList_ReturnFalse() {
        assertFalse(SSRFChecker.checkUrlInWhiteList("http://127.0.0.100/api", urlWhiteList));
    }

    @Test
    public void checkUrlWhiteList_UrlNotInWhiteList_ReturnFalse_2() {
        assertFalse(SSRFChecker.checkUrlInWhiteList("http://127.0.0.1/API", urlWhiteList));
    }

    @Test
    public void checkHostBlackList_EmptyBlackList_ReturnFalse() {
        assertTrue(SSRFChecker.checkHostNotInBlackList("127.0.0.1", Collections.emptyList()));
    }

    @Test
    public void checkHostBlackList_EmptyHost_ReturnTrue() {
        assertFalse(SSRFChecker.checkHostNotInBlackList("", urlBlackList));
    }

    @Test
    public void checkHostBlackList_HostInBlackList_ReturnTrue() {
        assertFalse(SSRFChecker.checkHostNotInBlackList("127.0.0.1", urlBlackList));
    }

    @Test
    public void checkHostBlackList_HostNotInBlackList_ReturnFalse() {
        assertTrue(SSRFChecker.checkHostNotInBlackList("127.0.0.2", urlBlackList));
    }

}
