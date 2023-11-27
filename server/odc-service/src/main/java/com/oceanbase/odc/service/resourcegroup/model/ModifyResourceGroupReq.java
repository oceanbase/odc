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
package com.oceanbase.odc.service.resourcegroup.model;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang.Validate;

import com.oceanbase.odc.common.validate.Name;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Request object, used to create or update a <code>ResourceGroup</code>
 *
 * @author yh263208
 * @date 2021-07-27 17:54
 * @since ODC-release_3.2.0
 */
@Getter
@Setter
@ToString
public class ModifyResourceGroupReq {
    /**
     * Name for a resource group
     */
    @Size(min = 1, max = 128, message = "Resource group name is out of range [1,128]")
    @Name(message = "Resource group name cannot start or end with whitespaces")
    private String name;
    /**
     * Notice for a resource group
     */
    @Size(max = 140, message = "Description is out of range [1,140]")
    private String description;
    /**
     * Flag to indicate whether the resource group is active
     */
    private boolean enabled = false;
    /**
     * Connection id list
     */
    @NotNull
    private List<ConnectionMetaInfo> connections;

    public ModifyResourceGroupReq() {}

    @Getter
    @Setter
    public static class ConnectionMetaInfo {
        private String name;
        private Long id;
        private Boolean enabled;

        public ConnectionMetaInfo() {}

        public ConnectionMetaInfo(ConnectionConfig connection) {
            Validate.notNull(connection, "connection can not be null for OdcConnectionMetaInfo");
            this.name = connection.getName();
            this.id = connection.getId();
            this.enabled = connection.getEnabled();
        }

        public ConnectionMetaInfo(ResourceIdentifier identifier) {
            Validate.notNull(identifier, "ResourceIdentifier can not be null for OdcConnectionMetaInfo");
            this.id = identifier.getResourceId();
        }

    }

}
