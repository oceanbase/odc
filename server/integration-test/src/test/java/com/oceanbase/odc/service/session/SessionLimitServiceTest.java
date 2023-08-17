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
package com.oceanbase.odc.service.session;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;
import com.oceanbase.odc.service.session.SessionLimitService.SessionLimitResp;

public class SessionLimitServiceTest extends ServiceTestEnv {
    @Autowired
    private SessionLimitService sessionLimitService;

    @Autowired
    private SessionProperties sessionProperties;

    @Autowired
    private AuthenticationFacade authenticationFacade;

    @Before
    public void setUp() {
        sessionProperties.setUserMaxCount(5);
        for (int i = 1; i < 5; i++) {
            sessionLimitService.updateTotalUserCountMap(Integer.toString(i));
        }
    }

    @Test
    public void testIsResourceAvailable_NotFull() {
        Assert.assertTrue(sessionLimitService.isResourceAvailable());
    }

    @Test
    public void testIsResourceAvailable_Full() {
        sessionLimitService.updateTotalUserCountMap(Integer.toString(5));
        Assert.assertFalse(sessionLimitService.isResourceAvailable());
        sessionLimitService.revokeUser(Integer.toString(5));
    }

    @Test
    public void testGetUserLineupStatus_NotFull() {
        SessionLimitResp userLineupStatus = sessionLimitService.getUserLineupStatus();
        Assert.assertTrue(userLineupStatus.isStatus());
        sessionLimitService.revokeUser(authenticationFacade.currentUserIdStr());
    }

    @Test
    public void testGetUserLineupStatus_Full_NotAllow() {
        sessionLimitService.updateTotalUserCountMap(Integer.toString(5));
        SessionLimitResp userLineupStatus = sessionLimitService.getUserLineupStatus();
        Assert.assertEquals(1, userLineupStatus.getWaitNum());
        sessionLimitService.revokeUser(Integer.toString(5));
        sessionLimitService.pollUserFromWaitQueue();
        sessionLimitService.revokeUser(authenticationFacade.currentUserIdStr());
    }

    @Test
    public void testGetUserLineupStatus_Full_Allow() {
        sessionLimitService.updateTotalUserCountMap(authenticationFacade.currentUserIdStr());
        SessionLimitResp userLineupStatus = sessionLimitService.getUserLineupStatus();
        Assert.assertTrue(userLineupStatus.isStatus());
        sessionLimitService.revokeUser(authenticationFacade.currentUserIdStr());
    }

}
