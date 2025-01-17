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
package com.oceanbase.odc.metadb.resource;

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

import com.oceanbase.odc.service.resource.ResourceID;
import com.oceanbase.odc.service.resource.ResourceState;

import lombok.Data;
import lombok.ToString;

/**
 * resource table definition for task
 *
 * @author longpeng.zlp
 * @date 2024/8/14 17:37
 */
@Data
@Entity
@ToString
@Table(name = "resource_resource")
public class ResourceEntity {
    public static final String CREATE_TIME = "createTime";
    public static final String STATUS = "status";
    public static final String TYPE = "resourceType";

    /**
     * id for task
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * record insertion time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false,
            columnDefinition = "datetime NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private Date createTime;

    /**
     * record modification time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false,
            columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Date updateTime;

    /**
     * region of the resource, eg beijing/shanghai
     */
    @Column(name = "region", updatable = false, nullable = false)
    private String region;

    /**
     * group of the resource, eg k8s cluster
     */
    @Column(name = "group_name", updatable = false, nullable = false)
    private String groupName;


    /**
     * namespace of the resource, eg k8s namespace
     */
    @Column(name = "namespace", updatable = false, nullable = false)
    private String namespace;

    /**
     * name of the resource, eg k8s pod name
     */
    @Column(name = "name", updatable = false, nullable = false)
    private String resourceName;

    /**
     * Resource type, equals to {@link ResourceID#getType()}
     */
    @Column(name = "resource_type", updatable = false, nullable = false)
    private String resourceType;

    /**
     * endpoint of the resource to access
     */
    @Column(name = "endpoint", updatable = false)
    private String endpoint;

    /**
     * resource status, enum: ResourceState
     */
    @Enumerated(value = EnumType.STRING)
    @Column(name = STATUS, nullable = false)
    private ResourceState status;

    @Column(name = "resource_properties")
    private String resourceProperties;
}
