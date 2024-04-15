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

import java.util.List;

public class DefaultTagServiceFacadeImpl implements TagServiceFacade {

    public static final String DEFAULT_SINGLE_USER_TAG_LABEL_KEY = "oceanbase_odc_whitelist";
    private final TagService tagService;

    public DefaultTagServiceFacadeImpl(TagService tagService) {
        this.tagService = tagService;
    }


    @Override
    public boolean checkOSCEnabled(Long userId) {
        List<String> tags = tagService.getUserTags(userId, DEFAULT_SINGLE_USER_TAG_LABEL_KEY);
        return tags.stream().anyMatch(TagNames.OSC_ENABLED::equals);
    }


}
