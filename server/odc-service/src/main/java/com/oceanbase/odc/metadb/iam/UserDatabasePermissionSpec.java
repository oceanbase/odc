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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.common.jpa.SpecificationUtil;
import com.oceanbase.odc.core.shared.constant.AuthorizationType;
import com.oceanbase.odc.service.permission.database.model.DatabasePermissionType;
import com.oceanbase.odc.service.permission.database.model.ExpirationStatusFilter;

/**
 * @author gaoda.xy
 * @date 2024/1/11 17:49
 */
public class UserDatabasePermissionSpec {

    private static final int EXPIRING_DAYS = 7;

    public static Specification<UserDatabasePermissionEntity> projectIdEqual(Long projectId) {
        return SpecificationUtil.columnEqual(UserDatabasePermissionEntity_.PROJECT_ID, projectId);
    }

    public static Specification<UserDatabasePermissionEntity> userIdEqual(Long userId) {
        return SpecificationUtil.columnEqual(UserDatabasePermissionEntity_.USER_ID, userId);
    }

    public static Specification<UserDatabasePermissionEntity> organizationIdEqual(Long organizationId) {
        return SpecificationUtil.columnEqual(UserDatabasePermissionEntity_.ORGANIZATION_ID, organizationId);
    }

    public static Specification<UserDatabasePermissionEntity> ticketIdEqual(Long ticketId) {
        return SpecificationUtil.columnEqual(UserDatabasePermissionEntity_.TICKET_ID, ticketId);
    }

    public static Specification<UserDatabasePermissionEntity> databaseNameLike(String fuzzyDatabaseName) {
        return SpecificationUtil.columnLike(UserDatabasePermissionEntity_.DATABASE_NAME, fuzzyDatabaseName);
    }

    public static Specification<UserDatabasePermissionEntity> dataSourceNameLike(String fuzzyDataSourceName) {
        return SpecificationUtil.columnLike(UserDatabasePermissionEntity_.DATA_SOURCE_NAME, fuzzyDataSourceName);
    }

    public static Specification<UserDatabasePermissionEntity> typeIn(Collection<DatabasePermissionType> types) {
        Set<String> actions = new HashSet<>();
        if (CollectionUtils.isNotEmpty(types)) {
            types.forEach(type -> {
                actions.add(type.getAction());
            });
        }
        return SpecificationUtil.columnIn(UserDatabasePermissionEntity_.ACTION, actions);
    }

    public static Specification<UserDatabasePermissionEntity> authorizationTypeEqual(AuthorizationType type) {
        return SpecificationUtil.columnEqual(UserDatabasePermissionEntity_.AUTHORIZATION_TYPE, type);
    }

    public static Specification<UserDatabasePermissionEntity> filterByExpirationStatus(
            List<ExpirationStatusFilter> statuses, Date expireTimeThreshold) {
        if (CollectionUtils.isEmpty(statuses)) {
            return (root, query, builder) -> builder.conjunction();
        }
        List<Specification<UserDatabasePermissionEntity>> expireSpecList = new ArrayList<>();
        for (ExpirationStatusFilter status : statuses) {
            expireSpecList.add(getByExpirationStatusFilter(status, expireTimeThreshold));
        }
        Specification<UserDatabasePermissionEntity> expireSpec = expireSpecList.get(0);
        for (int i = 1; i < expireSpecList.size(); i++) {
            expireSpec = expireSpec.or(expireSpecList.get(i));
        }
        return expireSpec;
    }

    private static Specification<UserDatabasePermissionEntity> getByExpirationStatusFilter(
            ExpirationStatusFilter status, Date date) {
        switch (status) {
            case EXPIRED:
                return expireTimeBefore(date);
            case EXPIRING:
                return expireTimeLate(date).and(expireTimeBefore(DateUtils.addDays(date, EXPIRING_DAYS)));
            case NOT_EXPIRED:
                return expireTimeLate(date);
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

    private static Specification<UserDatabasePermissionEntity> expireTimeBefore(Date date) {
        return SpecificationUtil.columnBefore(UserDatabasePermissionEntity_.EXPIRE_TIME, date);
    }

    private static Specification<UserDatabasePermissionEntity> expireTimeLate(Date date) {
        return SpecificationUtil.columnLate(UserDatabasePermissionEntity_.EXPIRE_TIME, date);
    }

}
