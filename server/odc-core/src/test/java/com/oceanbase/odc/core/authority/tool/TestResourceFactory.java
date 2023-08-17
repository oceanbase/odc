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
package com.oceanbase.odc.core.authority.tool;

import com.oceanbase.odc.core.authority.model.SecurityResource;

import lombok.Getter;
import lombok.Setter;

public class TestResourceFactory {

    public static ProviderTestResource getResource_1() {
        return new ProviderTestResource("1", "odc");
    }

    public static ProviderTestResource getResource_2() {
        return new ProviderTestResource("2", "odc");
    }

    public static ProviderTestResource getResource_3() {
        return new ProviderTestResource("2", "odc");
    }

    public static ProviderTestResourceNest getNestResource_4() {
        ProviderTestResourceNest nest = new ProviderTestResourceNest("4", "odc");
        nest.setResource_1(getResource_1());
        nest.setResource_2(getResource_2());
        nest.setResource_3(getResource_3());
        return nest;
    }

    public static class ProviderTestResource implements SecurityResource {

        private final String resourceId;
        private final String resourceType;

        public ProviderTestResource(String resourceId, String resourceType) {
            this.resourceId = resourceId;
            this.resourceType = resourceType;
        }

        @Override
        public String resourceId() {
            return this.resourceId;
        }

        @Override
        public String resourceType() {
            return this.resourceType;
        }
    }

    @Getter
    @Setter
    public static class ProviderTestResourceNest implements SecurityResource {
        private ProviderTestResource resource_1;
        private ProviderTestResource resource_2;
        private ProviderTestResource resource_3;
        private final String resourceId;
        private final String resourceType;

        public ProviderTestResourceNest(String resourceId, String resourceType) {
            this.resourceId = resourceId;
            this.resourceType = resourceType;
        }

        @Override
        public String resourceId() {
            return this.resourceId;
        }

        @Override
        public String resourceType() {
            return this.resourceType;
        }
    }

}
