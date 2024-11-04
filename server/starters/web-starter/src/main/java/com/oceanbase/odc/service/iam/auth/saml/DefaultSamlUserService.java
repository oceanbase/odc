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
package com.oceanbase.odc.service.iam.auth.saml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.service.iam.auth.MappingRuleConvert;
import com.oceanbase.odc.service.iam.auth.SsoUserDetailService;
import com.oceanbase.odc.service.iam.auth.oauth2.MappingResult;
import com.oceanbase.odc.service.iam.model.User;

@Service
public class DefaultSamlUserService {

    @Autowired
    private SsoUserDetailService ssoUserDetailService;

    @Autowired
    private MappingRuleConvert mappingRuleConvert;

    public User loadUser(Saml2Authentication saml2Authentication) {
        MappingResult mappingResult = mappingRuleConvert.resolveSamlMappingResult(saml2Authentication);
        return ssoUserDetailService.getOrCreateUser(mappingResult);
    }


}
