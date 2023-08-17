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
import java.text.DecimalFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * for create jackson ObjectMapper original fork from ocp repo at 2021-2-5
 */
public class JacksonFactory {

    /**
     * 为避免 javascript 整形溢出，long 超过此阈值的转为 String 序列化
     */
    public static final long MAX_SAFE_LONG = (long) Math.pow(2, 53) - 1;
    public static final long MIN_SAFE_LONG = -MAX_SAFE_LONG;

    /**
     * 浮点数保留2位小数
     */
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    /**
     * 缺省的时间格式
     */
    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * refer findAndRegisterModules from:
     * https://github.com/FasterXML/jackson-modules-java8#registering-modules
     *
     * we do not enable JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS, as only long/double should be convert
     * as string for javascript
     *
     * @return ObjectMapper
     */
    public static ObjectMapper unsafeJsonMapper() {
        return JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(new Jdk8Module())
                .addModule(new JavaTimeModule())
                .addModule(longHandlingModule())
                .build();
    }

    /**
     * 自动处理敏感信息的ObjectMapper
     *
     * @return ObjectMapper对象
     * @see MaskOutput
     */
    public static ObjectMapper jsonMapper() {
        return unsafeJsonMapper().registerModule(JacksonModules.sensitiveTextHandling());
    }

    public static ObjectMapper yamlMapper() {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy());
        yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return yamlMapper;
    }

    private static Module longHandlingModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, new LongSerializer());
        module.addSerializer(long.class, new LongSerializer());
        module.addSerializer(long[].class, new LongArraySerializer());
        module.addSerializer(Double.class, new DoubleSerializer());
        module.addSerializer(double.class, new DoubleSerializer());
        module.addSerializer(Float.class, new FloatSerializer());
        module.addSerializer(float.class, new FloatSerializer());
        return module;
    }

    private static void writeLong(JsonGenerator gen, long value) throws IOException {
        if (value > MAX_SAFE_LONG || value < MIN_SAFE_LONG) {
            gen.writeString(String.valueOf(value));
        } else {
            gen.writeNumber(value);
        }
    }

    public static class LongSerializer extends JsonSerializer<Long> {

        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            writeLong(gen, value);
        }
    }

    public static class LongArraySerializer extends JsonSerializer<long[]> {

        @Override
        public void serialize(long[] value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeStartArray();
            for (long v : value) {
                writeLong(gen, v);
            }
            gen.writeEndArray();
        }
    }

    public static class FloatSerializer extends JsonSerializer<Float> {

        @Override
        public void serialize(Float value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeNumber(DECIMAL_FORMAT.format(value));
            }
        }
    }

    public static class DoubleSerializer extends JsonSerializer<Double> {

        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeNumber(DECIMAL_FORMAT.format(value));
            }
        }
    }

}
