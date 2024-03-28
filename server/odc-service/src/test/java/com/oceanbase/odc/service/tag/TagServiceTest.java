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
package com.oceanbase.odc.service.tag;

import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.tag.DefaultTagService.UserTagItem;

public class TagServiceTest {

    SystemConfigService systemConfigService;

    TagServiceFacade tagServiceFacade;

    TagService tagService;

    @Before
    public void init() {
        systemConfigService = Mockito.mock(SystemConfigService.class);
        tagService = new DefaultTagService(systemConfigService);
        tagServiceFacade = new DefaultTagServiceFacadeImpl(tagService);
    }

    @Test
    public void test_tagService_getTag() {
        List<UserTagItem> userTagItems = new ArrayList<>();
        UserTagItem userTagItem = new UserTagItem(1L, "userTag", "osc_enabled");
        userTagItems.add(userTagItem);
        Mockito.when(systemConfigService.queryCacheByKey(anyString()))
                .thenReturn(new Configuration("", JsonUtils.toJson(userTagItems)));
        Assert.assertEquals(1, tagService.getUserTags(1L, "userTag").size());
    }

    @Test
    public void test_check_osc_enabled() {
        List<UserTagItem> userTagItems = new ArrayList<>();
        UserTagItem userTagItem = new UserTagItem(1L, "userTag", "osc_enabled");
        userTagItems.add(userTagItem);
        Mockito.when(systemConfigService.queryCacheByKey(anyString()))
                .thenReturn(new Configuration("", JsonUtils.toJson(userTagItems)));
        Assert.assertTrue(tagServiceFacade.checkOSCEnabled(1L));
    }

}
