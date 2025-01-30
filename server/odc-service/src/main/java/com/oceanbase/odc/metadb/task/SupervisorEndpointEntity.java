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
package com.oceanbase.odc.metadb.task;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import com.oceanbase.odc.service.task.supervisor.endpoint.SupervisorEndpoint;

import lombok.Data;

/**
 * entity for supervisor endpoint
 * 
 * @author longpeng.zlp
 * @date 2024/11/29 15:32
 */
@Data
@Entity
@Table(name = "supervisor_endpoint")
public class SupervisorEndpointEntity {

    /**
     * Id for supervisor endpoint
     */
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * host of supervisor endpoint
     */
    @Column(name = "host", nullable = false)
    private String host;

    /**
     * port of supervisor endpoint
     */
    @Column(name = "port", nullable = false)
    private Integer port;

    /**
     * status of supervisor endpoint, candidate value is
     * PREPARING,AVAILABLE,DESTROYED,UNAVAILABLE，ABANDON
     */
    @Column(name = "status", nullable = false)
    private String status;

    /**
     * load of supervisor endpoint, for task allocate
     */
    @Column(name = "loads", nullable = false)
    private Integer loads;

    /**
     * resourceID related to resource_resource, -1 means not related to any resource
     */
    @Column(name = "resource_id", nullable = false)
    private Long resourceID;

    /**
     * resource region of resource
     */
    @Column(name = "resource_region", nullable = false)
    private String resourceRegion;

    /**
     * resource region of resource
     */
    @Column(name = "resource_group", nullable = false)
    private String resourceGroup;

    /**
     * Record insertion time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "create_time", insertable = false, updatable = false,
            columnDefinition = "datetime NOT NULL DEFAULT CURRENT_TIMESTAMP")
    private Date createTime;

    /**
     * Record modification time
     */
    @Generated(GenerationTime.ALWAYS)
    @Column(name = "update_time", insertable = false, updatable = false,
            columnDefinition = "datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Date updateTime;

    public SupervisorEndpoint getEndpoint() {
        return new SupervisorEndpoint(host, port);
    }
}
