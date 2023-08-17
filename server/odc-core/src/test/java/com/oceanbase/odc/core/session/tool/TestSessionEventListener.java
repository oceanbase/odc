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
package com.oceanbase.odc.core.session.tool;

import org.junit.Assert;

import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.session.ConnectionSessionEventListener;

public class TestSessionEventListener implements ConnectionSessionEventListener {

    private boolean assertBoolean = false;

    public void doAssert() {
        Assert.assertFalse(assertBoolean);
    }

    @Override
    public void onCreateSucceed(ConnectionSession session) {
        assertBoolean = true;
    }

    @Override
    public void onCreateFailed(ConnectionSession session, Throwable e) {
        assertBoolean = true;
    }

    @Override
    public void onDeleteSucceed(ConnectionSession session) {
        assertBoolean = true;
    }

    @Override
    public void onDeleteFailed(String id, Throwable e) {
        assertBoolean = true;
    }

    @Override
    public void onGetSucceed(ConnectionSession session) {
        assertBoolean = true;
    }

    @Override
    public void onGetFailed(String id, Throwable e) {
        assertBoolean = true;
    }

    @Override
    public void onExpire(ConnectionSession session) {
        assertBoolean = true;
    }

    @Override
    public void onExpireSucceed(ConnectionSession session) {
        assertBoolean = true;
    }

    @Override
    public void onExpireFailed(ConnectionSession session, Throwable e) {
        assertBoolean = true;
    }

}
