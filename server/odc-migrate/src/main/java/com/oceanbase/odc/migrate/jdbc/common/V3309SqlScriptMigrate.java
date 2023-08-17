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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.migrate.JdbcMigratable;
import com.oceanbase.odc.core.migrate.Migratable;
import com.oceanbase.odc.metadb.objectstorage.BucketEntity;
import com.oceanbase.odc.metadb.objectstorage.BucketRepository;
import com.oceanbase.odc.service.common.util.SpringContextUtil;
import com.oceanbase.odc.service.objectstorage.ObjectStorageFacade;
import com.oceanbase.odc.service.objectstorage.model.ObjectMetadata;
import com.oceanbase.odc.service.script.model.ScriptConstants;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @Author: Lebie
 * @Date: 2022/3/28 下午8:46
 * @Description: []
 */
@Slf4j
@Migratable(version = "3.3.0.9", description = "Sql script migrate")
public class V3309SqlScriptMigrate implements JdbcMigratable {
    private JdbcTemplate jdbcTemplate;
    private ObjectStorageFacade objectStorageFacade;
    private BucketRepository bucketRepository;

    @Override
    public void migrate(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.objectStorageFacade = SpringContextUtil.getBean(ObjectStorageFacade.class);
        this.bucketRepository = (BucketRepository) SpringContextUtil.getBean("bucketRepository");
        migrateFromOdcSqlScriptToScriptMeta();
    }

    private void migrateFromOdcSqlScriptToScriptMeta() {
        String querySql =
                "SELECT t1.`id` as id, t1.`user_id` as user_id, t1.`script_name` as script_name, t1.`gmt_create` as gmt_create, t1.`gmt_modify` as gmt_modify, t1.`script_text` as script_text from `odc_sql_script` t1 "
                        + "left join `script_meta` t2 on t1.`id`=t2.`id` where t2.`object_id` is null";
        List<SqlScript> requiredMigrateScripts =
                jdbcTemplate.query(querySql, new BeanPropertyRowMapper<>(SqlScript.class));
        log.info("start migrating sql script, count={}", requiredMigrateScripts.size());
        int successCount = 0;
        for (SqlScript sqlScript : requiredMigrateScripts) {
            if (Objects.isNull(sqlScript.getUserId())) {
                continue;
            }
            if (StringUtils.isEmpty(sqlScript.getScriptName())) {
                sqlScript.setScriptName("untitled_script");
            }
            if (StringUtils.isEmpty(sqlScript.getScriptText())) {
                sqlScript.setScriptText("<empty_script>");
            }
            if (Objects.isNull(sqlScript.getGmtCreate())) {
                sqlScript.setGmtCreate(new Timestamp(System.currentTimeMillis()));
            }
            if (Objects.isNull(sqlScript.getGmtModify())) {
                sqlScript.setGmtModify(new Timestamp(System.currentTimeMillis()));
            }
            String bucketName = "script".concat(File.separator).concat(String.valueOf(sqlScript.getUserId()));
            if (!bucketRepository.findByName(bucketName).isPresent()) {
                BucketEntity bucketEntity = new BucketEntity();
                bucketEntity.setCreatorId(sqlScript.getUserId());
                bucketEntity.setName(bucketName);
                bucketRepository.saveAndFlush(bucketEntity);
            }
            ObjectMetadata objectMetadata = objectStorageFacade.putObject(bucketName,
                    sqlScript.getScriptName(), sqlScript.getUserId(),
                    sqlScript.getScriptText().getBytes(StandardCharsets.UTF_8).length,
                    IOUtils.toInputStream(sqlScript.getScriptText(), StandardCharsets.UTF_8));
            saveScriptMeta(objectMetadata, sqlScript.getGmtCreate(), sqlScript.getGmtModify(), sqlScript.getId(),
                    sqlScript.getScriptText());
            successCount++;
        }
        log.info("migrate sql script successfully, migrate count={}", successCount);
    }

    private void saveScriptMeta(ObjectMetadata objectMetadata, Date createTime, Date updateTime, long id,
            String content) {
        String insertSql = "insert into `script_meta`"
                + "(`id`, `object_id`, `creator_id`, `object_name`, `bucket_name`, `content_abstract`, `content_length`, "
                + "`create_time`, "
                + "`update_time`) VALUES (?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `id`=`id`";
        jdbcTemplate.update(insertSql, id, objectMetadata.getObjectId(), objectMetadata.getCreatorId(),
                objectMetadata.getObjectName(),
                objectMetadata.getBucketName(), getContentAbstract(content), objectMetadata.getTotalLength(),
                createTime,
                updateTime);
        log.debug("save script meta successfully, id={}", id);
    }

    private String getContentAbstract(String content) {
        if (StringUtils.isNotEmpty(content)) {
            content = (String) content.subSequence(0,
                    Math.min(content.length() - 1, ScriptConstants.CONTENT_ABSTRACT_LENGTH - 1));
        }
        return content;
    }

    @Data
    public static class SqlScript {
        private long id;
        private String objectId;
        private Long userId;
        private String scriptName;
        private String scriptText;
        private String scriptType;
        private Timestamp gmtCreate;
        private Timestamp gmtModify;
    }
}
