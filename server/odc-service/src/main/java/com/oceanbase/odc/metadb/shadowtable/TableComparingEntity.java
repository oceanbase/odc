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
package com.oceanbase.odc.metadb.shadowtable;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.shadowtable.model.TableComparingResult;

import lombok.Data;
import lombok.ToString;

/**
 * @Author: Lebie
 * @Date: 2022/9/19 下午2:33
 * @Description: []
 */
@Data
@Entity
@Table(name = "shadowtable_table_comparing")
@ToString
public class TableComparingEntity {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false)
    private Date createTime;

    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false)
    private Date updateTime;

    /**
     * related shadowtable comparing task id
     */
    @Column(name = "shadowtable_comparing_task_id", nullable = false)
    private Long comparingTaskId;

    @Column(name = "original_table_name", nullable = false)
    private String originalTableName;

    @Column(name = "dest_table_name", nullable = false)
    private String destTableName;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "comparing_result", nullable = false)
    private TableComparingResult comparingResult;

    @Column(name = "original_table_ddl")
    private String originalTableDDL;

    @Column(name = "dest_table_ddl")
    private String destTableDDL;

    @Column(name = "comparing_ddl")
    private String comparingDDL;

    @Column(name = "is_skipped")
    private Boolean skipped;
}
