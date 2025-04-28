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
package com.oceanbase.odc.service.onlineschemachange.oms.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.CheckerObjectStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.CheckerResultType;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsProjectStatusEnum;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OmsStepStatus;
import com.oceanbase.odc.service.onlineschemachange.oms.enums.OscStepName;

/**
 * @author yaobin
 * @date 2023-06-05
 * @since 4.2.0
 */
public class CustomEnumDeserializer {
    public static class CheckerResultTypeJsonDeserializer extends JsonDeserializer<CheckerResultType> {

        @Override
        public CheckerResultType deserialize(JsonParser jsonParser, DeserializationContext ctxt)
                throws IOException, JacksonException {

            String status = jsonParser.readValueAs(String.class);
            return getEnum(CheckerResultType.class, status,
                    CheckerResultType.UNKNOWN);
        }
    }

    public static class CheckerObjectStatusDeserializer extends JsonDeserializer<CheckerObjectStatus> {

        @Override
        public CheckerObjectStatus deserialize(JsonParser jsonParser, DeserializationContext ctxt)
                throws IOException, JacksonException {

            String status = jsonParser.readValueAs(String.class);
            return getEnum(CheckerObjectStatus.class, status,
                    CheckerObjectStatus.UNKNOWN);
        }
    }

    public static class ProjectStatusEnumDeserializer extends JsonDeserializer<OmsProjectStatusEnum> {

        @Override
        public OmsProjectStatusEnum deserialize(JsonParser jsonParser, DeserializationContext ctxt)
                throws IOException, JacksonException {

            String status = jsonParser.readValueAs(String.class);
            return getEnum(OmsProjectStatusEnum.class, status,
                    OmsProjectStatusEnum.UNKNOWN);
        }
    }

    public static class OmsStepNameDeserializer extends JsonDeserializer<OscStepName> {

        @Override
        public OscStepName deserialize(JsonParser jsonParser, DeserializationContext ctxt)
                throws IOException, JacksonException {

            String status = jsonParser.readValueAs(String.class);
            return getEnum(OscStepName.class, status, OscStepName.UNKNOWN);
        }
    }

    public static class OmsStepStatusDeserializer extends JsonDeserializer<OmsStepStatus> {

        @Override
        public OmsStepStatus deserialize(JsonParser jsonParser, DeserializationContext ctxt)
                throws IOException, JacksonException {

            String status = jsonParser.readValueAs(String.class);
            return getEnum(OmsStepStatus.class, status, OmsStepStatus.UNKNOWN);
        }
    }

    public static <E extends Enum<E>> E getEnum(Class<E> enumClass, String enumName, E defaultEnum) {
        if (enumName == null) {
            return defaultEnum;
        } else {
            try {
                return Enum.valueOf(enumClass, enumName);
            } catch (IllegalArgumentException ex) {
                return defaultEnum;
            }
        }
    }

}
