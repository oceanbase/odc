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
package com.oceanbase.odc.server;

import static com.oceanbase.odc.core.alarm.AlarmEventNames.SERVER_RESTART;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

import javax.validation.Validation;
import javax.validation.Validator;

import org.hibernate.validator.HibernateValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.security.SensitiveDataUtils;
import com.oceanbase.odc.common.util.SystemUtils;
import com.oceanbase.odc.core.alarm.AlarmUtils;
import com.oceanbase.odc.core.authority.interceptor.MethodAuthorizedPostProcessor;
import com.oceanbase.odc.migrate.AbstractMetaDBMigrate;
import com.oceanbase.odc.service.config.SystemConfigBootstrap;

import lombok.extern.slf4j.Slf4j;

/**
 * @author mogao.zj
 */
@Slf4j
@SpringBootApplication(scanBasePackages = {"com.oceanbase.odc"},
        exclude = CompositeMeterRegistryAutoConfiguration.class)
@EnableWebMvc
@Configuration
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ServletComponentScan(value = {"com.oceanbase.odc.server.web", "com.oceanbase.odc.web.trace"})
@EnableConfigServer
public class OdcServer {

    /**
     * make sure tomcat start after metadb migrate success
     */
    public OdcServer(@Qualifier("metadbMigrate") AbstractMetaDBMigrate metadbMigrate,
            SystemConfigBootstrap systemConfigBootstrap) {
        log.info("migrate instance name was {}", metadbMigrate.getClass().getName());
        log.info("odc server initialized.");
    }

    /**
     * springbooter 入口
     *
     * @param args
     */
    public static void main(String[] args) {
        AlarmUtils.alarm(SERVER_RESTART, LocalDateTime.now().toString());
        initEnv();
        System.setProperty("spring.cloud.bootstrap.enabled", "true");
        PluginSpringApplication.run(OdcServer.class, args);
        AlarmUtils.alarm(SERVER_RESTART, LocalDateTime.now().toString());
    }

    private static void initEnv() {
        log.info("odc server initializing...");

        Map<String, String> systemEnv = SystemUtils.getSystemEnv();
        log.info("systemEnv:\n{}", SensitiveDataUtils.mask(JsonUtils.prettyToJson(systemEnv)));
        Properties systemProperties = SystemUtils.getSystemProperties();
        log.info("systemProperties:\n{}", SensitiveDataUtils.mask(JsonUtils.prettyToJson(systemProperties)));

        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> log.info("Oceanbase Developer Center exits, systemInfo={}", SystemUtils.getSystemMemoryInfo())));

        // for work around path variable contains slash
        System.setProperty("org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH", "true");
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
    }

    @Bean
    public MethodValidationPostProcessor methodProcessor() {
        Validator validator = Validation.byProvider(HibernateValidator.class)
                .configure()
                .failFast(true)
                .buildValidatorFactory()
                .getValidator();
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        processor.setValidator(validator);
        return processor;
    }

    @Profile({"!clientMode"})
    @Bean
    public MethodAuthorizedPostProcessor authorizedMethodProcessor(ApplicationContext applicationContext) {
        return new MethodAuthorizedPostProcessor(applicationContext);
    }

}
