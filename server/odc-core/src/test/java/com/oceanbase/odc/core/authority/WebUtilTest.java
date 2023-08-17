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
package com.oceanbase.odc.core.authority;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.oceanbase.odc.core.authority.session.SecuritySession;
import com.oceanbase.odc.core.authority.util.WebUtil;

/**
 * Test object for {@link WebUtil}
 *
 * @author yh263208
 * @date 2021-07-23 10:16
 * @since ODC_release_3.2.0
 */
public class WebUtilTest {

    @Test
    public void testWebUtil() {
        SecuritySession session = Mockito.mock(SecuritySession.class);
        Mockito.when(session.getId()).thenReturn("abcde");
        Mockito.when(session.getTimeoutMillis()).thenReturn(100000L);
        Cookie cookie = WebUtil.generateSecurityCookie(session);
        Assert.assertNotNull(cookie);
        Assert.assertEquals(cookie.getValue(), session.getId());
        Assert.assertEquals(cookie.getMaxAge(),
                TimeUnit.SECONDS.convert(session.getTimeoutMillis(), TimeUnit.MILLISECONDS));
        Assert.assertTrue(cookie.isHttpOnly());
        Assert.assertEquals(cookie.getPath(), "/");
    }

    @Test
    public void testWebUtilWithIntMaxValue() {
        SecuritySession session = Mockito.mock(SecuritySession.class);
        Mockito.when(session.getId()).thenReturn("abcde");
        Mockito.when(session.getTimeoutMillis()).thenReturn(Integer.MAX_VALUE * 1000L + 100000);
        Cookie cookie = WebUtil.generateSecurityCookie(session);
        Assert.assertNotNull(cookie);
        Assert.assertEquals(cookie.getValue(), session.getId());
        Assert.assertEquals(cookie.getMaxAge(), Integer.MAX_VALUE);
        Assert.assertTrue(cookie.isHttpOnly());
        Assert.assertEquals(cookie.getPath(), "/");
    }

}
