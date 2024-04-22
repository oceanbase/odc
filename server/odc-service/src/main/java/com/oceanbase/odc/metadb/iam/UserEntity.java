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
package com.oceanbase.odc.metadb.iam;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.oceanbase.odc.common.security.PasswordUtils;
import com.oceanbase.odc.core.shared.constant.Cipher;
import com.oceanbase.odc.core.shared.constant.OdcConstants;
import com.oceanbase.odc.core.shared.constant.UserType;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wenniu.ly
 * @date 2021/6/25
 */

@Data
@Entity
@Table(name = "iam_user")
@ToString(exclude = {"password"})
@EqualsAndHashCode(exclude = {"updateTime", "createTime", "userCreateTime", "userUpdateTime"})
@Slf4j
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private UserType type;

    private String name;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    private String description;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    private String password;

    @Enumerated(value = EnumType.STRING)
    private Cipher cipher;

    @Column(name = "is_active", nullable = false)
    private boolean active;
    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "user_create_time", updatable = false)
    private Timestamp userCreateTime;

    @Column(name = "user_update_time")
    private Timestamp userUpdateTime;

    @Column(name = "create_time", insertable = false, updatable = false)
    private Timestamp createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private Timestamp updateTime;

    @Transient
    private Timestamp lastLoginTime;

    @Column(name = "is_builtin", nullable = false)
    private Boolean builtIn;

    @Column(name = "extra_properties_json")
    private String extraPropertiesJson;


    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static UserEntity autoCreatedEntity(@NonNull String account, @NonNull String name,
            @NonNull Long organizationId) {
        UserEntity userEntity = new UserEntity();
        userEntity.setType(UserType.USER);
        userEntity.setAccountName(account);
        userEntity.setName(name);
        userEntity.setPassword(passwordEncoder.encode(PasswordUtils.random()));
        userEntity.setCipher(Cipher.BCRYPT);
        userEntity.setEnabled(true);
        userEntity.setActive(true);
        userEntity.setBuiltIn(false);
        userEntity.setCreatorId(OdcConstants.DEFAULT_ADMIN_USER_ID);
        userEntity.setOrganizationId(organizationId);
        userEntity.setUserCreateTime(new Timestamp(System.currentTimeMillis()));
        userEntity.setUserUpdateTime(new Timestamp(System.currentTimeMillis()));
        return userEntity;
    }

}
