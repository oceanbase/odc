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
package com.oceanbase.odc.service.common.model;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.UserEntity;
import com.oceanbase.odc.service.iam.model.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * {@link InnerUser}
 *
 * @author yh263208
 * @date 2022-03-14 16:29
 * @since ODC_release_3.3.0
 */
@ToString
@EqualsAndHashCode
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InnerUser {
    private Long id;
    private String name;
    private String accountName;
    private List<String> roleNames;

    public InnerUser(@NonNull Long id) {
        this.id = id;
        this.name = null;
        this.accountName = null;
        this.roleNames = Collections.emptyList();
    }

    public InnerUser(@NonNull UserEntity user, List<RoleEntity> roleEntities) {
        this.id = user.getId();
        this.name = user.getName();
        this.accountName = user.getAccountName();
        if (roleEntities == null) {
            this.roleNames = Collections.emptyList();
        } else {
            this.roleNames = roleEntities.stream().map(RoleEntity::getName).collect(Collectors.toList());
        }
    }

    public InnerUser(@NonNull User user, List<RoleEntity> roleEntities) {
        this.id = user.getId();
        this.name = user.getName();
        this.accountName = user.getAccountName();
        if (roleEntities == null) {
            this.roleNames = Collections.emptyList();
        } else {
            this.roleNames = roleEntities.stream().map(RoleEntity::getName).collect(Collectors.toList());
        }
    }

    public InnerUser(@NonNull UserEntity user) {
        this(user, Collections.emptyList());
    }

    public static InnerUser of(@NonNull Long userId, Function<Long, UserEntity> getUserById,
            Function<Long, List<RoleEntity>> getRolesByUserId) {
        if (getUserById == null) {
            return new InnerUser(userId);
        }
        UserEntity userEntity = getUserById.apply(userId);
        if (userEntity != null && getRolesByUserId != null) {
            List<RoleEntity> roleEntities = getRolesByUserId.apply(userEntity.getId());
            if (CollectionUtils.isNotEmpty(roleEntities)) {
                return new InnerUser(userEntity, roleEntities);
            } else {
                return new InnerUser(userEntity);
            }
        } else if (userEntity != null) {
            return new InnerUser(userEntity);
        }
        return new InnerUser(userId);
    }

}
