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
package com.oceanbase.odc.config;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.i18n.I18nOutputSerializer;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.json.JacksonFactory;
import com.oceanbase.odc.common.json.JacksonModules;
import com.oceanbase.odc.common.json.JacksonModules.CustomOutputSerializer;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.service.connection.CloudMetadataClient;
import com.oceanbase.odc.service.connection.model.OBDatabaseUser;
import com.oceanbase.odc.service.connection.model.OBInstance;
import com.oceanbase.odc.service.connection.model.OBTenant;
import com.oceanbase.odc.service.connection.model.OBTenantEndpoint;
import com.oceanbase.odc.service.connection.model.OceanBaseAccessMode;
import com.oceanbase.odc.service.encryption.SensitivePropertyHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * define some special bean construction
 *
 * @author yizhou.xw
 * @version : BeanConfiguration.java, v 0.1 2021-02-03 14:08
 */
@Slf4j
@Configuration
public class BeanConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper(SensitivePropertyHandler sensitivePropertyHandler) {
        CustomOutputSerializer customOutputSerializer = new CustomOutputSerializer()
                .addSerializer(Internationalizable.class, new I18nOutputSerializer());
        return JacksonFactory.unsafeJsonMapper()
                .registerModule(JacksonModules.sensitiveInputHandling(sensitivePropertyHandler::decrypt))
                .registerModule(JacksonModules.customOutputHandling(customOutputSerializer));
    }

    /**
     * Spring security use StrictHttpFirewall by default, however special characters in url requires be
     * supported due object name for database has no limitation, e.g. '%' (url encoded as %25) was not
     * supported by default, replace default HttpFirewall to DefaultHttpFirewall to fix this issue.
     * TODO: API redesign, avoid malicious URL
     */
    @Bean
    public HttpFirewall defaultHttpFirewall() {
        DefaultHttpFirewall defaultHttpFirewall = new DefaultHttpFirewall();
        defaultHttpFirewall.setAllowUrlEncodedSlash(true);
        return defaultHttpFirewall;
    }

    @Bean
    @ConditionalOnMissingBean
    public CloudMetadataClient cloudMetadataClient() {
        return new NullCloudMetadataClient();
    }

    @Bean
    @Primary
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        return serializer;
    }

    @Slf4j
    public static class NullCloudMetadataClient implements CloudMetadataClient {

        @PostConstruct
        public void init() {
            log.info("CloudMetadataClient not found, will use NullCloudMetadataClient");
        }

        @Override
        public boolean supportsCloudMetadata() {
            return false;
        }

        @Override
        public boolean supportsTenantInstance() {
            return false;
        }

        @Override
        public boolean needsOBTenantName() {
            return false;
        }

        @Override
        public boolean needsSysTenantUser() {
            return false;
        }

        @Override
        public boolean includeClusterNameForJdbcUsername() {
            return true;
        }

        @Override
        public OceanBaseAccessMode oceanbaseAccessMode() {
            return OceanBaseAccessMode.DIRECT;
        }

        @Override
        public List<OBInstance> listInstances() {
            throw new UnsupportedException("CloudMetadata not supported");
        }

        @Override
        public List<OBTenant> listTenants(String instanceId) {
            throw new UnsupportedException("CloudMetadata not supported");
        }

        @Override
        public OBTenant getTenant(@NotBlank String instanceId,
                @NotBlank String tenantId) {
            throw new UnsupportedException("getTenant not supported in non-could");
        }

        @Override
        public List<OBDatabaseUser> listDatabaseUsers(String instanceId, String tenantId) {
            throw new UnsupportedException("CloudMetadata not supported");
        }

        @Override
        public OBTenantEndpoint getTenantEndpoint(String instanceId, String tenantId) {
            OBTenantEndpoint endpoint = new OBTenantEndpoint();
            endpoint.setAccessMode(OceanBaseAccessMode.DIRECT);
            return endpoint;
        }

        @Override
        public OBDatabaseUser getSysTenantUser(String instanceId) {
            throw new UnsupportedException("CloudMetadata not supported");
        }
    }

}
