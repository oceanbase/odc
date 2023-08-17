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
package com.oceanbase.odc.metadb.datasecurity;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.common.jpa.JsonListConverter;
import com.oceanbase.odc.service.datasecurity.model.SensitiveLevel;
import com.oceanbase.odc.service.datasecurity.model.SensitiveRuleType;

import lombok.Data;

/**
 * @author gaoda.xy
 * @date 2023/5/11 10:47
 */
@Data
@Entity
@Table(name = "data_security_sensitive_rule")
public class SensitiveRuleEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_enabled", nullable = false)
    private Boolean enabled;

    @Column(name = "is_builtin", nullable = false)
    private Boolean builtin;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SensitiveRuleType type;

    @Column(name = "database_regex_expression")
    private String databaseRegexExpression;

    @Column(name = "table_regex_expression")
    private String tableRegexExpression;

    @Column(name = "column_regex_expression")
    private String columnRegexExpression;

    @Column(name = "column_comment_regex_expression")
    private String columnCommentRegexExpression;

    @Column(name = "groovy_script")
    private String groovyScript;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "path_includes")
    private List<String> pathIncludes;

    @Convert(converter = JsonListConverter.class)
    @Column(name = "path_excludes")
    private List<String> pathExcludes;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "sensitive_level", nullable = false)
    private SensitiveLevel level;

    @Column(name = "masking_algorithm_id", nullable = false)
    private Long maskingAlgorithmId;

    @Column(name = "description")
    private String description;

    @Column(name = "creator_id", nullable = false, updatable = false)
    private Long creatorId;

    @Column(name = "project_id", nullable = false, updatable = false)
    private Long projectId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private Long organizationId;

    @Generated(value = GenerationTime.ALWAYS)
    @Column(name = "create_time", nullable = false, insertable = false, updatable = false)
    private Date createTime;

    @Generated(value = GenerationTime.ALWAYS)
    @Column(name = "update_time", nullable = false, insertable = false, updatable = false)
    private Date updateTime;

}
