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
public class UserTablePermissionSpec {

    private static final int EXPIRING_DAYS = 7;

    public static Specification<UserTablePermissionEntity> projectIdEqual(Long projectId) {
        return SpecificationUtil.columnEqual(UserTablePermissionEntity_.PROJECT_ID, projectId);
    }

    public static Specification<UserTablePermissionEntity> userIdEqual(Long userId) {
        return SpecificationUtil.columnEqual(UserTablePermissionEntity_.USER_ID, userId);
    }

    public static Specification<UserTablePermissionEntity> organizationIdEqual(Long organizationId) {
        return SpecificationUtil.columnEqual(UserTablePermissionEntity_.ORGANIZATION_ID, organizationId);
    }

    public static Specification<UserTablePermissionEntity> ticketIdEqual(Long ticketId) {
        return SpecificationUtil.columnEqual(UserTablePermissionEntity_.TICKET_ID, ticketId);
    }

    public static Specification<UserTablePermissionEntity> databaseNameLike(String fuzzyDatabaseName) {
        return SpecificationUtil.columnLike(UserTablePermissionEntity_.DATABASE_NAME, fuzzyDatabaseName);
    }

    public static Specification<UserTablePermissionEntity> tableNameLike(String fuzzyTableName) {
        return SpecificationUtil.columnLike(UserTablePermissionEntity_.TABLE_NAME, fuzzyTableName);
    }

    public static Specification<UserTablePermissionEntity> dataSourceNameLike(String fuzzyDataSourceName) {
        return SpecificationUtil.columnLike(UserTablePermissionEntity_.DATA_SOURCE_NAME, fuzzyDataSourceName);
    }

    public static Specification<UserTablePermissionEntity> dataSourceId(Long datasourceId) {
        return SpecificationUtil.columnEqual(UserTablePermissionEntity_.DATA_SOURCE_ID, datasourceId);
    }

    public static Specification<UserTablePermissionEntity> typeIn(Collection<DatabasePermissionType> types) {
        Set<String> actions = new HashSet<>();
        if (CollectionUtils.isNotEmpty(types)) {
            types.forEach(type -> {
                actions.add(type.getAction());
            });
        }
        return SpecificationUtil.columnIn(UserTablePermissionEntity_.ACTION, actions);
    }

    public static Specification<UserTablePermissionEntity> authorizationTypeEqual(AuthorizationType type) {
        return SpecificationUtil.columnEqual(UserTablePermissionEntity_.AUTHORIZATION_TYPE, type);
    }

    public static Specification<UserTablePermissionEntity> filterByExpirationStatus(
            List<ExpirationStatusFilter> statuses, Date expireTimeThreshold) {
        if (CollectionUtils.isEmpty(statuses)) {
            return (root, query, builder) -> builder.conjunction();
        }
        List<Specification<UserTablePermissionEntity>> expireSpecList = new ArrayList<>();
        for (ExpirationStatusFilter status : statuses) {
            expireSpecList.add(getByExpirationStatusFilter(status, expireTimeThreshold));
        }
        Specification<UserTablePermissionEntity> expireSpec = expireSpecList.get(0);
        for (int i = 1; i < expireSpecList.size(); i++) {
            expireSpec = expireSpec.or(expireSpecList.get(i));
        }
        return expireSpec;
    }

    private static Specification<UserTablePermissionEntity> getByExpirationStatusFilter(
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

    private static Specification<UserTablePermissionEntity> expireTimeBefore(Date date) {
        return SpecificationUtil.columnBefore(UserTablePermissionEntity_.EXPIRE_TIME, date);
    }

    private static Specification<UserTablePermissionEntity> expireTimeLate(Date date) {
        return SpecificationUtil.columnLate(UserTablePermissionEntity_.EXPIRE_TIME, date);
    }

}
