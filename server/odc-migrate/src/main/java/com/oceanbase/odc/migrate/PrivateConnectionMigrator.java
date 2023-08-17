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
package com.oceanbase.odc.migrate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.odc.common.crypto.TextEncryptor;
import com.oceanbase.odc.common.util.ResourceUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.shared.constant.ConnectionVisibleScope;
import com.oceanbase.odc.metadb.connection.ConnectionConfigRepository;
import com.oceanbase.odc.metadb.connection.ConnectionEntity;
import com.oceanbase.odc.metadb.connection.ConnectionLabelRelationEntity;
import com.oceanbase.odc.metadb.connection.ConnectionLabelRelationRepository;
import com.oceanbase.odc.metadb.connection.ConnectionSetTopEntity;
import com.oceanbase.odc.metadb.connection.ConnectionSetTopRepository;
import com.oceanbase.odc.service.connection.model.PropertiesKeys;
import com.oceanbase.odc.service.encryption.EncryptionFacade;

import lombok.extern.slf4j.Slf4j;

/**
 * Migrator for private connection
 *
 * @author yh263208
 * @date 2021-09-26 11:59
 * @since ODC_release_3.2.1
 */
@Slf4j
@Component
public class PrivateConnectionMigrator {

    private String sql;
    @Autowired
    private ConnectionConfigRepository repository;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private ConnectionLabelRelationRepository labelRelationRepository;
    @Autowired
    private ConnectionSetTopRepository setTopRepository;

    @Autowired
    private EncryptionFacade encryptionFacade;

    @PostConstruct
    public void setUp() throws IOException {
        InputStream input = ResourceUtils.getFileAsStream("init-config/runtime/private_connection_migrate.sql");
        this.sql = IOUtils.toString(input, StandardCharsets.UTF_8);
    }

    public List<ConnectionEntity> migrateRoleUsersResource(Long oldUserId, Long newUserId, Long organizationId) {
        // 迁移连接
        List<ConnectionEntity> connectionEntities = repository.findByVisibleScopeAndCreatorId(
                ConnectionVisibleScope.PRIVATE, oldUserId).stream().filter(Objects::nonNull)
                .map(entity -> generateNewConnectionEntity(oldUserId, newUserId, entity)).collect(Collectors.toList());
        connectionEntities = repository.saveAll(connectionEntities);
        Map<Long, Map<String, String>> properties = connectionEntities.stream().collect(
                Collectors.toMap(ConnectionEntity::getId, ConnectionEntity::getProperties));
        // 迁移置顶
        try {
            List<ConnectionSetTopEntity> entities = properties.entrySet().stream().filter(
                    e -> StringUtils.isNotEmpty(e.getValue().get(PropertiesKeys.SET_TOP))
                            && e.getValue().get(PropertiesKeys.SET_TOP).equals("true"))
                    .map(e -> {
                        ConnectionSetTopEntity entity = new ConnectionSetTopEntity();
                        entity.setUserId(newUserId);
                        entity.setConnectionId(e.getKey());
                        return entity;
                    }).collect(Collectors.toList());
            this.setTopRepository.saveAll(entities);
        } catch (Exception e) {
            log.warn("Failed to migrate set top info", e);
        }

        // 迁移标签
        try {
            List<ConnectionLabelRelationEntity> entities = properties.entrySet().stream().filter(
                    e -> StringUtils.isNumeric(e.getValue().get(PropertiesKeys.LABEL_ID)))
                    .map(e -> {
                        ConnectionLabelRelationEntity entity = new ConnectionLabelRelationEntity();
                        entity.setUserId(newUserId);
                        entity.setConnectionId(e.getKey());
                        entity.setLabelId(Long.parseLong(e.getValue().get(PropertiesKeys.LABEL_ID)));
                        return entity;
                    }).collect(Collectors.toList());
            this.labelRelationRepository.saveAll(entities);
        } catch (Exception e) {
            log.warn("Failed to migrate label info", e);
        }

        log.info("The migration connection is successful, oleUserid={}, newUserId={}, organizationId={}, affectRows={}",
                oldUserId, newUserId, organizationId, connectionEntities.size());
        return connectionEntities;
    }

    public List<ConnectionEntity> migrate(Long oldUserId, Long newUserId, Long organizationId) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        Map<String, Long> connName2LabelId = new HashMap<>();
        List<String> setTopConnNames = new ArrayList<>();
        List<ConnectionEntity> connectionEntities = jdbcTemplate
                .query(sql, new Object[] {oldUserId}, new ConnectionRowMapper(setTopConnNames, connName2LabelId))
                .stream().filter(Objects::nonNull).peek(
                        connectionEntity -> {
                            connectionEntity.setId(null);
                            connectionEntity.setOwnerId(newUserId);
                            connectionEntity.setCreatorId(newUserId);
                            connectionEntity.setOrganizationId(organizationId);
                            connectionEntity.setEnabled(true);
                            connectionEntity.setTemp(false);
                        })
                .collect(Collectors.toList());
        connectionEntities = repository.saveAll(connectionEntities);
        Map<String, Long> connName2ConnId = connectionEntities.stream()
                .collect(Collectors.toMap(ConnectionEntity::getName, ConnectionEntity::getId));
        try {
            List<ConnectionSetTopEntity> entities = setTopConnNames.stream()
                    .filter(connName2ConnId::containsKey).map(s -> {
                        ConnectionSetTopEntity entity = new ConnectionSetTopEntity();
                        entity.setUserId(newUserId);
                        entity.setConnectionId(connName2ConnId.get(s));
                        return entity;
                    }).collect(Collectors.toList());
            this.setTopRepository.saveAll(entities);
        } catch (Exception e) {
            log.warn("Failed to migrate set top info", e);
        }
        try {
            List<ConnectionLabelRelationEntity> entities = connName2LabelId.entrySet().stream()
                    .filter(e -> connName2ConnId.containsKey(e.getKey())).map(e -> {
                        ConnectionLabelRelationEntity entity = new ConnectionLabelRelationEntity();
                        entity.setUserId(newUserId);
                        entity.setConnectionId(connName2ConnId.get(e.getKey()));
                        entity.setLabelId(e.getValue());
                        return entity;
                    }).collect(Collectors.toList());
            this.labelRelationRepository.saveAll(entities);
        } catch (Exception e) {
            log.warn("Failed to migrate label info", e);
        }
        log.info("The migration connection is successful, oleUserid={}, newUserId={}, organizationId={}, affectRows={}",
                oldUserId, newUserId, organizationId, connectionEntities.size());
        return connectionEntities;
    }

    private ConnectionEntity generateNewConnectionEntity(Long oldUserId, Long newUserId, ConnectionEntity old) {

        TextEncryptor oldUserEncryptor = encryptionFacade.userEncryptor(oldUserId, old.getSalt());
        TextEncryptor newUserEncryptor = encryptionFacade.userEncryptor(newUserId, old.getSalt());

        ConnectionEntity newConnection = new ConnectionEntity();
        BeanUtils.copyProperties(old, newConnection);
        newConnection.setId(null);
        newConnection.setOwnerId(newUserId);
        newConnection.setCreatorId(newUserId);
        newConnection.setEnabled(true);
        newConnection.setTemp(false);
        // 解密并重新加密
        newConnection
                .setPasswordEncrypted(newUserEncryptor.encrypt(oldUserEncryptor.decrypt(old.getPasswordEncrypted())));
        newConnection.setSysTenantPasswordEncrypted(
                newUserEncryptor.encrypt(oldUserEncryptor.decrypt(old.getSysTenantPasswordEncrypted())));
        newConnection.setReadonlyPasswordEncrypted(
                newUserEncryptor.encrypt(oldUserEncryptor.decrypt(old.getReadonlyPasswordEncrypted())));

        return newConnection;
    }

}


@Slf4j
class ConnectionRowMapper extends BeanPropertyRowMapper<ConnectionEntity> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<String> setTopConnNames;
    private final Map<String, Long> connName2LabelId;

    public ConnectionRowMapper(List<String> setTopConnNames, Map<String, Long> connName2LabelId) {
        super(ConnectionEntity.class);
        this.setTopConnNames = setTopConnNames;
        this.connName2LabelId = connName2LabelId;
    }

    @Override
    public ConnectionEntity mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        try {
            ConnectionEntity connectionEntity = super.mapRow(resultSet, rowNumber);
            assert connectionEntity != null;
            connectionEntity.setPasswordSaved("1".equals(resultSet.getString("is_password_saved")));
            String propertyJson = resultSet.getString("properties_json");
            connectionEntity.setProperties(new HashMap<>());
            if (propertyJson != null) {
                try {
                    Map<String, String> properties = mapper.readValue(
                            propertyJson, new TypeReference<Map<String, String>>() {});
                    connectionEntity.setProperties(properties);
                    String value = String.valueOf(properties.get(PropertiesKeys.SET_TOP));
                    String connName = connectionEntity.getName();
                    if ("true".equalsIgnoreCase(value)) {
                        this.setTopConnNames.add(connName);
                    }
                    String labelIdStr = String.valueOf(properties.get(PropertiesKeys.LABEL_ID));
                    if (StringUtils.isNotBlank(labelIdStr)) {
                        this.connName2LabelId.put(connName, Long.valueOf(labelIdStr));
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to set properties json", e);
                    connectionEntity.setProperties(new HashMap<>());
                }
            }
            return connectionEntity;
        } catch (Exception e) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < columnCount; j++) {
                builder.append("[")
                        .append(metaData.getColumnLabel(j + 1))
                        .append("]=")
                        .append(resultSet.getString(j + 1))
                        .append(", ");
            }
            log.error("The original connection data value is illegal and cannot be converted, row={}",
                    builder, e);
        }
        return null;
    }

}
