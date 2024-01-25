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

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

/**
 * ODC basic information
 *
 * @author yizhou.xw
 * @version : OdcInfo.java, v 0.1 2021-02-05 19:25
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OdcInfo {
    /**
     * ODC 版本号
     */
    private String version;

    /**
     * ODC 构建时间
     */
    private OffsetDateTime buildTime;

    /**
     * ODC 服务启动时间
     */
    private OffsetDateTime startTime;

    /**
     * 是否开启的三方登录
     */
    private boolean ssoLoginEnabled;

    /**
     * 显示第三方登录的按钮的文案
     */
    private String ssoLoginName;

    /**
     * 第三方登录类型 /OAUTH2、OIDC、LDAP
     */
    private String ssoLoginType;

    /**
     * 是否开启密码登录
     */
    private boolean passwordLoginEnabled;

    /**
     * Web 资源路径，可能是本地路径，也可能是 CDN 路径
     */
    private String webResourceLocation;

    /**
     * 当前ODC启动模式
     */
    private String[] profiles;

    /**
     * 用户支持群地址
     */
    private String supportGroupQRCodeUrl;

    /**
     * 用户支持反馈邮箱
     */
    private String supportEmail;

    /**
     * 用户支持地址
     */
    private String supportUrl;

    /**
     * 首页文案
     */
    private String homePageText;

    /**
     * 模拟数据条数限制
     */
    private long mockDataMaxRowCount;

    /**
     * 脚本查看/编辑内容的最大长度，单位为字节，默认 20 MB
     */
    private long maxScriptEditLength;

    /**
     * 脚本上传的最大长度，单位为字节，默认 250 MB
     */
    private long maxScriptUploadLength;

    /**
     * 任务相关文件在 {@code ODC} 上最大保留的小时数
     */
    private int fileExpireHours;

    /**
     * 是否开启前端埋点，默认关闭
     */
    private boolean spmEnabled;

    /**
     * 是否开启登录验证码
     */
    private boolean captchaEnabled;

    /**
     * 是否开启session限流，默认为否
     */
    private boolean sessionLimitEnabled;

    /**
     * 是否开启教程功能，默认为否
     */
    private boolean tutorialEnabled;

    /**
     * 是否隐藏权限申请，默认为否
     */
    private boolean applyPermissionHidden;

    /**
     * odc当前展示的Title
     */
    private String odcTitle;

    /**
     * 创建新用户默认关联的角色
     */
    private List<String> defaultRoles;
}
