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

import javax.sql.DataSource;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.integration.IntegrationEntity;
import com.oceanbase.odc.service.integration.model.Oauth2Parameter;
import com.oceanbase.odc.service.integration.model.SSOIntegrationConfig;

@Migratable(version = "4.3.2.5",
        description = "Process Historical sso integrations config")
public class V4325Oauth2UseStateParamDefaultFalse implements JdbcMigratable {

    @Override
    public void migrate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<IntegrationEntity> integrationEntities = getIntegration(jdbcTemplate);
        integrationEntities.forEach(i -> {
            SSOIntegrationConfig ssoIntegrationConfig =
                    JsonUtils.fromJson(i.getConfiguration(), SSOIntegrationConfig.class);
            if (ssoIntegrationConfig.isOauth2OrOidc()) {
                Oauth2Parameter ssoParameter = (Oauth2Parameter) ssoIntegrationConfig.getSsoParameter();
                ssoParameter.setUseStateParams(false);
                String newConfig = JsonUtils.toJson(ssoIntegrationConfig);
                updateConfig(jdbcTemplate, newConfig, i.getId());
            }
        });

    }

    private List<IntegrationEntity> getIntegration(JdbcTemplate jdbcTemplate) {
        String sql = "select * from integration_integration where type = 'SSO'";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(IntegrationEntity.class));
    }

    private void updateConfig(JdbcTemplate jdbcTemplate, String config, Long id) {
        String sql = "update integration_integration set configuration = ? where id = ?";
        jdbcTemplate.update(sql, config, id);
    }
}
