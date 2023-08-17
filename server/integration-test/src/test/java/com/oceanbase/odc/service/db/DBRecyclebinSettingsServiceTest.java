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
package com.oceanbase.odc.service.db;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.TestConnectionUtil;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.db.DBRecyclebinSettingsService.RecyclebinSettings;

public class DBRecyclebinSettingsServiceTest extends ServiceTestEnv {

    @Autowired
    private DBRecyclebinSettingsService recyclebinSettingsService;
    private final ConnectionSession session = TestConnectionUtil.getTestConnectionSession(ConnectType.OB_MYSQL);

    @Test
    public void get() {
        RecyclebinSettings settings = recyclebinSettingsService.get(session);
        Assert.assertNotNull(settings);
    }

    @Test
    public void update_SetRecyclebinEnabledTrue_True() {
        RecyclebinSettings settings = new RecyclebinSettings();
        settings.setRecyclebinEnabled(true);
        RecyclebinSettings updated = recyclebinSettingsService.update(Arrays.asList(session, session), settings);
        Assert.assertTrue(updated.getRecyclebinEnabled());
    }

    @Test
    public void update_SetTruncateFlashbackEnabledFalse_False() {
        RecyclebinSettings settings = new RecyclebinSettings();
        settings.setTruncateFlashbackEnabled(false);
        RecyclebinSettings updated = recyclebinSettingsService.update(Collections.singletonList(session), settings);
        Assert.assertFalse(updated.getTruncateFlashbackEnabled());
    }

    @Test
    public void update_SetAllSettings_AllMatch() {
        RecyclebinSettings settings = new RecyclebinSettings();
        settings.setRecyclebinEnabled(false);
        settings.setTruncateFlashbackEnabled(true);
        RecyclebinSettings updated = recyclebinSettingsService.update(Collections.singletonList(session), settings);
        Assert.assertFalse(updated.getRecyclebinEnabled());
        Assert.assertTrue(updated.getTruncateFlashbackEnabled());
    }

}
