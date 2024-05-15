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
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/8/2
 */
@Slf4j
@Configuration
public class CommonSecurityProperties {
    private static String[] buildInAuthWhitelist = new String[] {
            "/api/v1/heartbeat/isHealthy",
            "/api/v1/heartbeat/getMetaStatus",
            "/api/v1/time",
            "/api/v1/info",
            "/api/v1/build-info",
            "/api/v1/git-info",
            "/api/v1/status",
            "/api/v1/sso-login",
            "/api/v1/sso-logout",
            "/api/v1/pop/info/GetInfo",
            "/api/v1/pop/info/GetOdcTime",
            "/api/v2/iam/users/*/activate",
            "/api/v2/iam/captcha",
            "/api/v1/other/decode",
            "/api/v2/bastion/encryption/decrypt",
            "/api/v2/internal/file/downloadImportFile",
            "/api/v2/info",
            "/api/v2/encryption/publicKey"};

    private static final String[] STATIC_RESOURCES = new String[] {
            "/",
            "/error",
            "/*.html",
            "/*.html/*",
            "/*.js",
            "/**/*.js",
            "/umi.*.css",
            "/*.css",
            "/**/*.css",
            "/img/*",
            "/img/en-us/*",
            "/img/zh-cn/*",
            "/help/*",
            "/help-pdf/*",
            "/help-doc/*",
            "/*.woff",
            "/**/*.woff",
            "/template/en-us/*",
            "/template/zh-cn/*",
            "/template/zh-tw/*"};


    private static final String[] TASK_WHITE_LIST = new String[] {
            "/api/v2/task/heart",
            "/api/v2/task/result",
            "/api/v2/task/querySensitiveColumn"
    };

    private static final String LOGOUT_URI = "/api/v2/iam/logout";
    private static final String LOGIN_URI = "/api/v2/iam/login";
    private static final String LOGIN_PAGE = "/index.html";
    private static final String SESSION_COOKIE_KEY = "JSESSIONID";

    /**
     * 是否开启 CSRF 防护，默认开启
     */
    @Getter
    @Value("${odc.web.security.csrf.enabled:true}")
    private boolean csrfEnabled;

    /**
     * authType，字符串，用,分隔， buc和oauth2后面可以追加local
     */
    @Value("${odc.iam.auth.type}")
    private Set<String> authType;

    /**
     * 是否开启 CORS 支持，默认不支持，<br>
     * 前端通过 CDN 部署且后端 API 部署的 domain 和前端访问 domain 不一致时需开启本项配置
     */
    @Getter
    @Value("${odc.web.security.cors.enabled:false}")
    private boolean corsEnabled;

    @Getter
    @Value("${odc.web.security.basic-authentication.enabled:false}")
    private boolean basicAuthenticationEnabled;

    /**
     * CORS 允许的 origins domain 列表，当 odc.web.security.cors.enabled=true 时配置有效
     */
    @Getter
    @Value("${odc.web.security.cors.allowedOrigins:*}")
    private List<String> corsAllowedOrigins;

    @PostConstruct
    public void init() {
        log.info("Common security properties initialized, "
                + "csrfEnabled={}, corsEnabled={}, corsAllowedOrigins={}",
                csrfEnabled, corsEnabled, corsAllowedOrigins);
    }

    public String[] getAuthWhitelist() {
        return ArrayUtils.addAll(buildInAuthWhitelist, TASK_WHITE_LIST);
    }

    public String[] getTaskWhiteList() {
        return ArrayUtils.addAll(TASK_WHITE_LIST);
    }

    public String[] getStaticResources() {
        return ArrayUtils.addAll(STATIC_RESOURCES);
    }

    public String getLogoutUri() {
        return LOGOUT_URI;
    }

    public String getLoginUri() {
        return LOGIN_URI;
    }

    public String getLoginPage() {
        return LOGIN_PAGE;
    }

    public String getSessionCookieKey() {
        return SESSION_COOKIE_KEY;
    }

    public boolean authTypeContainsLocal() {
        return authType.contains("local");
    }
}
