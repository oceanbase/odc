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
package com.oceanbase.odc.migrate.jdbc.common;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.iam.RoleEntity;
import com.oceanbase.odc.metadb.iam.RolePermissionEntity;
import com.oceanbase.odc.metadb.iam.UserRoleEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link V41018PermissionMigrate}
 *
 * @author yh263208
 * @date 2022-12-28 10:50
 * @since ODC_release_4.1.0
 */
@Slf4j
@Migratable(version = "4.1.0.18", description = "iam_role_permission migrate")
public class V41018PermissionMigrate implements JdbcMigratable {

    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String sql = "select * from iam_role where type='INTERNAL' and is_enabled=1";
        List<RoleEntity> roles = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(RoleEntity.class));
        if (roles.isEmpty()) {
            return;
        }
        String roleIds = roles.stream().map(r -> r.getId() + "").collect(Collectors.joining(","));
        sql = String.format("select * from iam_role_permission where role_id in (%s)", roleIds);
        List<RolePermissionEntity> roleRelation =
                jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(RolePermissionEntity.class));
        sql = String.format("select * from iam_user_role where role_id in (%s)", roleIds);
        List<UserRoleEntity> userRelation = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(UserRoleEntity.class));
        TransactionTemplate txTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        txTemplate.execute((TransactionCallback<Void>) t -> {
            try {
                Map<Long, List<UserRoleEntity>> roleId2Entities = userRelation.stream()
                        .collect(Collectors.groupingBy(UserRoleEntity::getRoleId));
                String insert =
                        "insert into iam_user_permission (user_id,permission_id,creator_id,organization_id) values(?,?,?,?)";
                List<Object[]> args = roleRelation.stream().filter(e -> roleId2Entities.get(e.getRoleId()) != null)
                        .flatMap(e -> roleId2Entities.get(e.getRoleId()).stream().map(ue -> new Long[] {
                                ue.getUserId(),
                                e.getPermissionId(),
                                e.getCreatorId(),
                                e.getOrganizationId()
                        })).collect(Collectors.toList());
                if (!args.isEmpty()) {
                    jdbcTemplate.batchUpdate(insert, args);
                }
                String delete = String.format("delete from iam_role where id in (%s)", roleIds);
                jdbcTemplate.update(delete);
                delete = String.format("delete from iam_role_permission where role_id in (%s)", roleIds);
                jdbcTemplate.update(delete);
                delete = String.format("delete from iam_user_role where role_id in (%s)", roleIds);
                jdbcTemplate.update(delete);
            } catch (Exception e) {
                t.setRollbackOnly();
                throw e;
            }
            return null;
        });
    }

}
