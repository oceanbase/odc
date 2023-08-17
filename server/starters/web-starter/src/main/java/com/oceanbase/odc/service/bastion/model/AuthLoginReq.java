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
package com.oceanbase.odc.service.bastion.model;

import java.io.BufferedReader;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.json.SensitiveInput;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class AuthLoginReq {
    /**
     * username, same as ODC User.accountName
     */
    private String username;

    @SensitiveInput
    private String password;

    /**
     * token 值非空时，触发堡垒机账号集成流程
     */
    private String token;

    public static AuthLoginReq fromRequest(HttpServletRequest request, ObjectMapper objectMapper) {
        try {
            BufferedReader reader = request.getReader();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String parsedReq = sb.toString();
            if (StringUtils.isEmpty(parsedReq)) {
                throw new NullPointerException("Request body is empty");
            }
            return objectMapper.readValue(parsedReq, AuthLoginReq.class);
        } catch (Exception e) {
            log.error("Fail to get authReq from the request", e);
            throw new InternalAuthenticationServiceException("Failed to get AuthReq from the request", e);
        }
    }
}
