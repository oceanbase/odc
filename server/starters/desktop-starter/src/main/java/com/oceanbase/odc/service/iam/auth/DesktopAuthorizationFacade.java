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
package com.oceanbase.odc.service.iam.auth;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.ResourceType;

/**
 * Implements for <code>AuthorizationFacade</code> in client mode
 *
 * @author yh263208
 * @date 2021-09-13 14:28
 * @since ODC_release_3.2.0
 * @see AuthorizationFacade
 */
@Service
@Profile("clientMode")
@SkipAuthorize("odc internal usage")
public class DesktopAuthorizationFacade extends DefaultAuthorizationFacade {

    @Override
    public Set<String> getAllPermittedActions(Principal principal, ResourceType resourceType, String resourceId) {
        return new HashSet<>(Collections.singletonList("*"));
    }

}
