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
package com.oceanbase.odc.service.iam.model;

import java.security.Principal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.oceanbase.odc.common.json.SensitiveInput;
import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.core.shared.PreConditions;
import com.oceanbase.odc.core.shared.constant.OrganizationType;
import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.core.shared.constant.UserType;
import com.oceanbase.odc.metadb.iam.UserEntity;

import lombok.Data;
import lombok.ToString;

/**
 * @author wenniu.ly
 * @date 2021/7/21
 */

@Data
@ToString(exclude = {"password"})
public class User implements Principal, UserDetails, SecurityResource, OrganizationIsolated, OidcUser {

    private static final long serialVersionUID = -7525670432276629968L;

    @JsonProperty(access = Access.READ_ONLY)
    private Long id;
    @JsonProperty(access = Access.READ_ONLY)
    private UserType type;

    private String name;
    private String accountName;
    private String description;

    @JsonProperty(access = Access.WRITE_ONLY)
    @SensitiveInput
    private String password;

    private List<Long> roleIds;
    private boolean enabled;

    @JsonProperty(access = Access.READ_ONLY)
    private boolean active;
    @JsonProperty(access = Access.READ_ONLY)
    private Long creatorId;
    @JsonProperty(access = Access.READ_ONLY)
    private String creatorName;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp createTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp updateTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp lastLoginTime;
    @JsonProperty(access = Access.READ_ONLY)
    private Timestamp loginTime;
    /**
     * Permissions will be returned when includePermissions=true or detailCurrentUser
     */
    @JsonProperty(access = Access.READ_ONLY)
    private List<PermissionConfig> resourceManagementPermissions;
    @JsonProperty(access = Access.READ_ONLY)
    private List<PermissionConfig> systemOperationPermissions;

    @JsonProperty(access = Access.READ_ONLY)
    private List<Role> roles;
    /**
     * current organization id of this user, bind to session level
     */
    @JsonProperty(access = Access.READ_ONLY)
    private Long organizationId;

    @JsonProperty(access = Access.READ_ONLY)
    private OrganizationType organizationType;

    @JsonProperty(access = Access.READ_ONLY)
    private Set<String> authorizedActions;
    @JsonProperty(access = Access.READ_ONLY)
    private Boolean builtIn;
    private String extraProperties;

    /** only private aliyun use and will put value after ram authorization **/
    @JsonIgnore
    private List<RamPermission> ramPermissions;

    /**
     * Only for public cloud, Used to display the main account id
     */
    @JsonIgnore
    private String parentUid;

    public User() {}

    public static User of(Long id) {
        PreConditions.notNull(id, "id");
        User user = new User();
        user.setId(id);
        return user;
    }

    public User(UserEntity userEntity) {
        this.id = userEntity.getId();
        this.type = userEntity.getType();
        this.name = userEntity.getName();
        this.accountName = userEntity.getAccountName();
        this.password = userEntity.getPassword();
        this.active = userEntity.isActive();
        this.enabled = userEntity.isEnabled();
        this.creatorId = userEntity.getCreatorId();
        this.createTime = userEntity.getUserCreateTime();
        this.updateTime = userEntity.getUserUpdateTime();
        this.description = userEntity.getDescription();
        this.organizationId = userEntity.getOrganizationId();
        this.lastLoginTime = userEntity.getLastLoginTime();
        this.loginTime = userEntity.getLastLoginTime();
        this.builtIn = userEntity.getBuiltIn();
        this.extraProperties = userEntity.getExtraPropertiesJson();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getUsername() {
        return this.accountName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public String resourceId() {
        return this.id.toString();
    }

    @Override
    public String resourceType() {
        return ResourceType.ODC_USER.name();
    }

    @Override
    public Long organizationId() {
        return this.organizationId;
    }

    @Override
    public Long id() {
        return this.id;
    }

    @Override
    public Map<String, Object> getClaims() {
        return new HashMap<>();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return null;
    }

    @Override
    public OidcIdToken getIdToken() {
        return null;
    }
}
