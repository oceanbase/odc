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
package com.oceanbase.odc.core.flow.builder;

import org.flowable.bpmn.model.FormProperty;

import com.oceanbase.odc.core.flow.model.FlowableFormType;
import com.oceanbase.odc.core.flow.util.FlowIdGenerators;

import lombok.NonNull;

/**
 * Builder util to build some models for flowable
 *
 * @author yh263208
 * @date 2022-01-18 20:36
 * @since ODC_release_3.3.0
 */
public class FlowableModelBuilder {

    public static FormPropertyBuilder getPropertyBuilder() {
        return new FormPropertyBuilder();
    }

    /**
     * Builder to build {@link FormProperty}
     *
     * @author yh263208
     * @date 2022-01-18 20:51
     * @since ODC_release_3.3.0
     */
    public static class FormPropertyBuilder {
        private String formType;
        private String name;
        private String id;
        private final FormProperty property;

        private FormPropertyBuilder() {
            property = new FormProperty();
            property.setReadable(true);
            property.setWriteable(true);
            property.setRequired(true);
        }

        public FormPropertyBuilder withRequired(boolean required) {
            this.property.setRequired(required);
            return this;
        }

        public FormPropertyBuilder withReadable(boolean readable) {
            this.property.setReadable(readable);
            return this;
        }

        public FormPropertyBuilder withWriteable(boolean writeable) {
            this.property.setWriteable(writeable);
            return this;
        }

        public FormPropertyBuilder withName(@NonNull String name) {
            this.name = name;
            return this;
        }

        public FormPropertyBuilder withFormType(@NonNull FlowableFormType formType) {
            this.formType = formType.getTypeName();
            return this;
        }

        public FormProperty build() {
            if (this.formType == null) {
                throw new IllegalStateException("Form type is necessary");
            }
            if (this.name == null) {
                throw new IllegalStateException("Form name is necessary");
            }
            this.id = FlowIdGenerators.uniqueStringIdGenerator().generateId();
            property.setName(name);
            property.setId(name);
            property.setType(formType);
            return property;
        }
    }

}
