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

import static com.oceanbase.odc.core.shared.constant.OdcConstants.DEFAULT_MASK_VALUE;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.oceanbase.odc.common.i18n.I18nOutputSerializer;
import com.oceanbase.odc.common.i18n.Internationalizable;
import com.oceanbase.odc.common.json.JacksonFactory;
import com.oceanbase.odc.common.json.JacksonModules;
import com.oceanbase.odc.common.json.JacksonModules.CustomOutputSerializer;
import com.oceanbase.odc.common.json.JacksonModules.SensitiveOutputSerializer;
import com.oceanbase.odc.common.json.NormalDialectTypeOutput;
import com.oceanbase.odc.common.json.SensitiveOutput;
import com.oceanbase.odc.core.shared.constant.DialectType;
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
                .addSerializer(Internationalizable.class, new I18nOutputSerializer())
                .addSerializer(SensitiveOutput.class, new SensitiveOutputSerializer(input -> DEFAULT_MASK_VALUE));
        SimpleModule dialectTypeModule =
                new SimpleModule().addSerializer(DialectType.class, new DialectTypeOutputSerializer());
        return JacksonFactory.unsafeJsonMapper()
                .registerModule(JacksonModules.sensitiveInputHandling(sensitivePropertyHandler::decrypt))
                .registerModule(JacksonModules.customOutputHandling(customOutputSerializer))
                .registerModule(dialectTypeModule);
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
        public List<OBInstance> listInstances(Long organizationId) {
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

        @Override
        public boolean supportsCloudParentUid() {
            return false;
        }

        @Override
        public OBDatabaseUser createDatabaseUser(String instanceId, String tenantId, OBDatabaseUser user,
                Map<String, String> roles) {
            throw new UnsupportedException("CloudMetadata not supported");
        }

        @Override
        public void deleteDatabaseUsers(String instanceId, String tenantId, List<String> users) {
            throw new UnsupportedException("CloudMetadata not supported");
        }

        @Override
        public com.oceanbase.odc.service.config.model.Configuration getConfiguration(String projectId, String key) {
            throw new UnsupportedException("CloudMetadata not supported");
        }
    }

    /**
     * In order to adapt to the fact that there is no ODP_SHARDING_OB_MYSQL in DialectType of the
     * front-end, ODP_SHARDING_OB_MYSQL is converted to OB_MYSQL during serialization. Will be removed
     * in the future
     */
    @Deprecated
    private static class DialectTypeOutputSerializer extends JsonSerializer<DialectType> implements
            ContextualSerializer {

        @Override
        public void serialize(DialectType dialectType, JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider) throws IOException {
            if (Objects.nonNull(dialectType)) {
                if (dialectType.equals(DialectType.ODP_SHARDING_OB_MYSQL)) {
                    jsonGenerator.writeString(DialectType.OB_MYSQL.name());
                } else {
                    jsonGenerator.writeString(dialectType.name());
                }
            } else {
                jsonGenerator.writeNull();
            }
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider serializerProvider,
                BeanProperty beanProperty)
                throws JsonMappingException {
            if (Objects.isNull(beanProperty)) {
                return new JsonValueSerializer();
            }
            NormalDialectTypeOutput normalOutput = beanProperty.getAnnotation(NormalDialectTypeOutput.class);
            if (Objects.isNull(normalOutput)) {
                normalOutput = beanProperty.getContextAnnotation(NormalDialectTypeOutput.class);
            }
            return Objects.isNull(normalOutput) ? this : new JsonValueSerializer();
        }

        private static class JsonValueSerializer extends JsonSerializer<DialectType> {
            @Override
            public void serialize(DialectType dialectType, JsonGenerator jsonGenerator,
                    SerializerProvider serializerProvider) throws IOException {
                if (Objects.nonNull(dialectType)) {
                    jsonGenerator.writeString(dialectType.name());
                } else {
                    jsonGenerator.writeNull();
                }
            }
        }
    }

}
