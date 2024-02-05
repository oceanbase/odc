/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.metadb.stateroute;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.oceanbase.odc.config.jpa.JsonType;
import com.oceanbase.odc.service.state.RouteInfo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Entity
@Table(name = "state_route")
@AllArgsConstructor
@NoArgsConstructor
@TypeDefs({@TypeDef(name = "Json", typeClass = JsonType.class)})
public class StateRouteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, insertable = false, updatable = false)
    private Date createTime;

    private String stateName;

    private String stateId;

    @Type(type = "Json")
    private RouteInfo routeInfo;

    public StateRouteEntity(String stateName, String stateId, RouteInfo routeInfo) {
        this.stateName = stateName;
        this.stateId = stateId;
        this.routeInfo = routeInfo;
    }
}
