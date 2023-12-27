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
package com.oceanbase.odc.service.info;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Service;

import com.oceanbase.odc.common.util.ObjectUtil;
import com.oceanbase.odc.core.authority.util.SkipAuthorize;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.common.util.UrlUtils;
import com.oceanbase.odc.service.common.util.WebRequestUtils;
import com.oceanbase.odc.service.config.model.FeaturesProperties;
import com.oceanbase.odc.service.flow.task.model.FlowTaskProperties;
import com.oceanbase.odc.service.flow.task.model.MockProperties;
import com.oceanbase.odc.service.integration.IntegrationService;
import com.oceanbase.odc.service.lab.model.LabProperties;
import com.oceanbase.odc.service.script.model.ScriptProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yizhou.xw
 * @version : OdcInfoService.java, v 0.1 2021-02-05 19:13
 */
@Slf4j
@Service
@SkipAuthorize
public class OdcInfoService {

    @Autowired
    private BuildProperties buildProperties;

    @Autowired
    private GitProperties gitProperties;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private InfoProperties infoProperties;

    @Autowired
    private ScriptProperties scriptProperties;

    @Autowired
    private FlowTaskProperties flowTaskProperties;

    @Autowired
    private InfoAdapter infoAdapter;

    @Autowired
    private FeaturesProperties featuresProperties;

    @Autowired
    private LabProperties labProperties;

    @Autowired
    private MockProperties mockProperties;

    private OdcInfo staticOdcInfo;

    @Autowired
    private IntegrationService integrationService;

    @Value("${odc.iam.auth.type}")
    private Set<String> authType;

    @Value("${odc.iam.authentication.captcha.enabled:false}")
    private boolean captchaEnabled;

    @Value("${odc.iam.user.default-roles:}")
    private String defaultRolesString;

    @PostConstruct
    public void init() {
        staticOdcInfo = new OdcInfo();
        staticOdcInfo.setStartTime(
                instant2OffsetDateTime(Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime())));
        staticOdcInfo.setHomePageText(infoProperties.getHomePageText());
        staticOdcInfo.setSupportEmail(infoProperties.getSupportEmail());
        staticOdcInfo.setSupportUrl(infoProperties.getSupportUrl());
        staticOdcInfo.setMockDataMaxRowCount(mockProperties.getMaxRowCount());
        staticOdcInfo.setMaxScriptEditLength(scriptProperties.getMaxEditLength());
        staticOdcInfo.setMaxScriptUploadLength(scriptProperties.getMaxUploadLength());
        staticOdcInfo.setFileExpireHours(flowTaskProperties.getFileExpireHours());
        staticOdcInfo.setCaptchaEnabled(captchaEnabled);
        staticOdcInfo.setSpmEnabled(featuresProperties.isSpmEnabled());
        staticOdcInfo.setTutorialEnabled(labProperties.isTutorialEnabled());
        staticOdcInfo.setSessionLimitEnabled(labProperties.isSessionLimitEnabled());
        staticOdcInfo.setApplyPermissionHidden(labProperties.isApplyPermissionHidden());
        staticOdcInfo.setOdcTitle(labProperties.isLabEnabled() ? "OceanBase" : "");
        staticOdcInfo.setDefaultRoles(getDefaultRolesName());
    }

    public List<String> getDefaultRolesName() {
        return StringUtils.isBlank(defaultRolesString) ? new ArrayList<>()
                : Arrays.asList(defaultRolesString.split(","));
    }

    public OffsetDateTime time() {
        return OffsetDateTime.now();
    }

    public OdcInfo info() {
        OdcInfo odcInfo = ObjectUtil.deepCopy(this.staticOdcInfo, OdcInfo.class);
        String[] profiles = SpringContextUtil.getProfiles();
        odcInfo.setVersion(infoAdapter.getBuildVersion());
        odcInfo.setBuildTime(infoAdapter.getBuildTime());
        odcInfo.setProfiles(profiles);
        odcInfo.setPasswordLoginEnabled(this.infoAdapter.isPasswordLoginEnabled());
        odcInfo.setSsoLoginEnabled(Objects.nonNull(getLoginUrl()));
        odcInfo.setSsoLoginName(infoAdapter.ssoLoginName());
        odcInfo.setSupportGroupQRCodeUrl(getSupportGroupQRCodeUrl());
        return odcInfo;
    }

    public BuildProperties buildInfo() {
        return buildProperties;
    }

    public GitProperties gitInfo() {
        return gitProperties;
    }

    public String status() {
        return "okay";
    }

    public String getLoginUrl() {
        return infoAdapter.getLoginUrl(request);
    }

    public static String addOdcParameter(HttpServletRequest request, String redirect) {
        String odcBackUrl = request.getParameter(OdcConstants.ODC_BACK_URL_PARAM);
        if (WebRequestUtils.isRedirectUrlValid(request, odcBackUrl)) {
            redirect = UrlUtils.appendQueryParameter(redirect, OdcConstants.ODC_BACK_URL_PARAM, odcBackUrl);
        }
        String testLoginId = request.getParameter(OdcConstants.TEST_LOGIN_ID_PARAM);
        if (com.oceanbase.odc.common.util.StringUtils.isNotBlank(testLoginId)) {
            redirect = UrlUtils.appendQueryParameter(redirect, OdcConstants.TEST_LOGIN_ID_PARAM, testLoginId);
        }
        String testLoginType = request.getParameter(OdcConstants.TEST_LOGIN_TYPE);
        if (com.oceanbase.odc.common.util.StringUtils.isNotBlank(testLoginType)) {
            redirect = UrlUtils.appendQueryParameter(redirect, OdcConstants.TEST_LOGIN_TYPE, testLoginType);
        }
        return redirect;
    }


    public String getLogoutUrl() {
        return this.infoAdapter.getLogoutUrl(request);
    }

    public String getSupportGroupQRCodeUrl() {
        return this.infoAdapter.getSupportGroupQRCodeUrl();
    }

    private OffsetDateTime instant2OffsetDateTime(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
