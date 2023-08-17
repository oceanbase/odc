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

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.core.authority.exception.InvalidSessionException;
import com.oceanbase.odc.core.authority.session.DefaultSecuritySession;

/**
 * Test object for session object: {@link DefaultSecuritySession}
 *
 * @author yh263208
 * @date 2021-07-13 17:19
 * @since ODC_release_3.2.0
 */
public class DefaultSecuritySessionTest {

    @Test
    public void setAttribute_defaultSecuritySession_setSucceed() throws InvalidSessionException {
        DefaultSecuritySession session = new DefaultSecuritySession("127.0.0.1", 180000, null);
        session.setAttribute("name", "David");
        Assert.assertEquals(session.getAttribute("name"), "David");
    }

    @Test
    public void removeAttribute_removeExistedKey_removeSucceed() throws InvalidSessionException {
        DefaultSecuritySession session = new DefaultSecuritySession(180000);
        session.setAttribute("name", "David");
        session.removeAttribute("name");
        Assert.assertNull(session.getAttribute("name"));
    }

    @Test(expected = InvalidSessionException.class)
    public void removeAttribute_sessionIsExpired_removeFailed() throws InvalidSessionException, InterruptedException {
        DefaultSecuritySession session = new DefaultSecuritySession(10);
        session.setAttribute("name", "David");
        Thread.sleep(100);
        session.removeAttribute("name");
    }

    @Test(expected = InvalidSessionException.class)
    public void expire_expiredASession_expireSucceed() throws InvalidSessionException {
        DefaultSecuritySession session = new DefaultSecuritySession(180000);
        session.setAttribute("name", "David");
        session.expire();
        session.removeAttribute("name");
    }

    @Test
    public void touch_touchASession_touchSucceed() throws InvalidSessionException, InterruptedException {
        DefaultSecuritySession session = new DefaultSecuritySession(200);
        session.setAttribute("name", "David");
        Thread.sleep(150);
        session.touch();
        Thread.sleep(150);
        session.removeAttribute("name");
    }

    @Test(expected = InvalidSessionException.class)
    public void touch_touchAnExpiredSession_touchFailed() throws InvalidSessionException, InterruptedException {
        DefaultSecuritySession session = new DefaultSecuritySession(200);
        session.setAttribute("name", "David");
        Thread.sleep(250);
        session.touch();
    }

}
