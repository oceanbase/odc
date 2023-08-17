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
package com.oceanbase.odc.common.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;

import lombok.Getter;

/**
 * jackson modules factory, for build specific ObjectMapper
 */
public class JacksonModules {

    /**
     * module for handle sensitive text property should be masked while output
     */
    public static Module sensitiveTextHandling() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(String.class, new MaskOutputSerializer());
        return module;
    }

    /**
     * module for handle sensitive text property should be decrypted while input
     * 
     * @param decryptConverter the decryption converter
     */
    public static Module sensitiveInputHandling(Function<String, String> decryptConverter) {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(String.class, new SensitiveInputDeserializer(decryptConverter));
        return module;
    }

    /**
     * module for handle resource to be serialized while output
     */
    public static Module customOutputHandling(CustomOutputSerializer customOutputSerializer) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(String.class, customOutputSerializer);
        return module;
    }

    private static class SensitiveInputDeserializer extends JsonDeserializer<String> implements ContextualDeserializer {
        private final Function<String, String> inputConverter;

        private SensitiveInputDeserializer(Function<String, String> inputConverter) {
            this.inputConverter = inputConverter;
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String encryptedValue = p.readValueAs(String.class);
            return inputConverter.apply(encryptedValue);
        }

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty beanProperty) {
            if (Objects.isNull(beanProperty)) {
                return StringDeserializer.instance;
            }
            SensitiveInput sensitiveInput = beanProperty.getAnnotation(SensitiveInput.class);
            if (Objects.isNull(sensitiveInput)) {
                sensitiveInput = beanProperty.getContextAnnotation(SensitiveInput.class);
            }
            return Objects.isNull(sensitiveInput) ? StringDeserializer.instance : this;
        }
    }

    public static class SensitiveOutputSerializer extends JsonSerializer<String> {
        private final Function<String, String> outputConverter;

        public SensitiveOutputSerializer(Function<String, String> outputConverter) {
            this.outputConverter = outputConverter;
        }

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(outputConverter.apply(value));
        }
    }

    /**
     * 处理敏感字符串数据的数据
     */
    private static class MaskOutputSerializer extends JsonSerializer<String> implements ContextualSerializer {

        private static final String DEFAULT_MASK_VALUE = "******";
        @Getter
        private final String maskValue;

        public MaskOutputSerializer(String maskValue) {
            this.maskValue = maskValue;
        }

        public MaskOutputSerializer() {
            this(DEFAULT_MASK_VALUE);
        }

        @Override
        public void serialize(String s, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
            jsonGenerator.writeString(maskValue);
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider serializerProvider, BeanProperty beanProperty) {
            if (Objects.isNull(beanProperty)) {
                return new StringSerializer();
            }
            MaskOutput maskField = beanProperty.getAnnotation(MaskOutput.class);
            if (Objects.isNull(maskField)) {
                maskField = beanProperty.getContextAnnotation(MaskOutput.class);
            }
            return Objects.isNull(maskField) ? new StringSerializer() : new MaskOutputSerializer(maskField.value());
        }
    }

    public static class CustomOutputSerializer extends JsonSerializer<String> implements ContextualSerializer {
        private final Map<Class, JsonSerializer<String>> serializerCachedMap = new HashMap<>();

        public CustomOutputSerializer addSerializer(Class annotation, JsonSerializer<String> serializer) {
            serializerCachedMap.put(annotation, serializer);
            return this;
        }

        @Override
        public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
            if (Objects.isNull(property)) {
                return new StringSerializer();
            }
            for (Class annotation : serializerCachedMap.keySet()) {
                if (!Objects.isNull(property.getAnnotation(annotation))
                        || !Objects.isNull(property.getContextAnnotation(annotation))) {
                    return serializerCachedMap.get(annotation);
                }
            }
            return new StringSerializer();
        }

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) {}
    }

}
