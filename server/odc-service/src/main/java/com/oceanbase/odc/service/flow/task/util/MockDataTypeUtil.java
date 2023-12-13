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
package com.oceanbase.odc.service.flow.task.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;

/**
 * mock数据类型映射工具类
 *
 * @author yh263208
 * @date 201-01-20 20:21
 * @since ODC_release_2.4.0
 */
public class MockDataTypeUtil {
    /**
     * 用于维护数据类型和其分类之间的映射关系 key -> 数据类型，对应于真实的数据库类型 value ->
     * 数据类型分类，例如number属于数字类型(digit)，不同的数据分类需要使用不同的配置来描述因此需要对不同的数据类型进行分类
     */
    private static final Map<String, String> TYPE_MAP;

    static {
        TYPE_MAP = new HashMap<>();
        TYPE_MAP.put("OB_ORACLE_NUMBER", "DIGIT");
        TYPE_MAP.put("OB_ORACLE_BLOB", "BYTE");
        TYPE_MAP.put("OB_ORACLE_CLOB", "BYTE");
        TYPE_MAP.put("OB_ORACLE_RAW", "BYTE");
        TYPE_MAP.put("OB_ORACLE_TIMESTAMP_WITH_LOCAL_TIME_ZONE", "TIMESTAMP");
        TYPE_MAP.put("OB_ORACLE_TIMESTAMP_WITH_TIME_ZONE", "TIMESTAMP");
        TYPE_MAP.put("OB_ORACLE_TIMESTAMP", "TIMESTAMP");
        TYPE_MAP.put("OB_ORACLE_DATE", "DATE");
        TYPE_MAP.put("OB_ORACLE_INTERVAL_YEAR_TO_MONTH", "INTERVAL");
        TYPE_MAP.put("OB_ORACLE_INTERVAL_DAY_TO_SECOND", "INTERVAL");
        TYPE_MAP.put("OB_ORACLE_NVARCHAR", "CHAR");
        TYPE_MAP.put("OB_ORACLE_VARCHAR2", "CHAR");
        TYPE_MAP.put("OB_ORACLE_VARCHAR", "CHAR");
        TYPE_MAP.put("OB_ORACLE_CHAR", "CHAR");

        TYPE_MAP.put("MYSQL_TINYINT", "DIGIT");
        TYPE_MAP.put("MYSQL_TINYINT_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_SMALLINT", "DIGIT");
        TYPE_MAP.put("MYSQL_SMALLINT_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_MEDIUMINT", "DIGIT");
        TYPE_MAP.put("MYSQL_MEDIUMINT_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_INT", "DIGIT");
        TYPE_MAP.put("MYSQL_INT_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_BIGINT", "DIGIT");
        TYPE_MAP.put("MYSQL_BIGINT_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_DECIMAL", "DIGIT");
        TYPE_MAP.put("MYSQL_DECIMAL_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_FLOAT", "DIGIT");
        TYPE_MAP.put("MYSQL_FLOAT_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_NUMBER", "DIGIT");
        TYPE_MAP.put("MYSQL_NUMBER_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_DOUBLE", "DIGIT");
        TYPE_MAP.put("MYSQL_DOUBLE_UNSIGNED", "DIGIT");
        TYPE_MAP.put("MYSQL_BIT", "DIGIT");

        TYPE_MAP.put("MYSQL_CHAR", "CHAR");
        TYPE_MAP.put("MYSQL_VARCHAR", "CHAR");
        TYPE_MAP.put("MYSQL_TINYTEXT", "CHAR");
        TYPE_MAP.put("MYSQL_TEXT", "CHAR");
        TYPE_MAP.put("MYSQL_MEDIUMTEXT", "CHAR");
        TYPE_MAP.put("MYSQL_LONGTEXT", "CHAR");

        TYPE_MAP.put("MYSQL_TINYBLOB", "BYTE");
        TYPE_MAP.put("MYSQL_BLOB", "BYTE");
        TYPE_MAP.put("MYSQL_MEDIUMBLOB", "BYTE");
        TYPE_MAP.put("MYSQL_LONGBLOB", "BYTE");
        TYPE_MAP.put("MYSQL_BINARY", "BYTE");
        TYPE_MAP.put("MYSQL_VARBINARY", "BYTE");

        TYPE_MAP.put("MYSQL_DATE", "DATE");
        TYPE_MAP.put("MYSQL_YEAR", "DATE");

        TYPE_MAP.put("MYSQL_TIMESTAMP", "TIMESTAMP");
        TYPE_MAP.put("MYSQL_TIME", "TIMESTAMP");
        TYPE_MAP.put("MYSQL_DATETIME", "TIMESTAMP");
    }

    public static String getType(DialectType dialectType, String dataType) {
        if (dialectType == null || dataType == null) {
            return null;
        }
        String key;
        if (dialectType.isMysql()) {
            key = String.format("%s_%s", DialectType.MYSQL.name(), dataType);
        } else if (DialectType.OB_ORACLE.equals(dialectType)) {
            key = String.format("%s_%s", DialectType.OB_ORACLE, dataType);
        } else {
            throw new UnsupportedException(String.format("Dialect type %s has not been supported yet", dialectType));
        }
        return TYPE_MAP.get(key);
    }

    /**
     * 由于前后端对于接口的理解稍有差异，一些数据生成器的名称及其参数需要做映射转换
     *
     * @param type 数据类型大类
     * @param typeConfig 类型配置对象
     */
    public static void processTypeConfig(String type, Map<String, Object> typeConfig) {
        if ("CHAR".equalsIgnoreCase(type)) {
            if ("RANDOM_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Long lowValue = Long.valueOf(typeConfig.get("lowValue").toString());
                Long highValue = Long.valueOf(typeConfig.get("highValue").toString());
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                typeConfig.put("lowValue", null);
                typeConfig.put("highValue", null);
                param.putIfAbsent("startTime", lowValue);
                param.putIfAbsent("endTime", highValue);
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "RANDOM_DATE_CHAR_GENERATOR");
            } else if ("FIX_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "FIX_DATE_CHAR_GENERATOR");
            } else if ("STEP_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Long lowValue = Long.valueOf(typeConfig.get("lowValue").toString());
                typeConfig.put("lowValue", null);
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                Long step = Long.valueOf(param.get("step").toString());
                if (step > 0) {
                    param.putIfAbsent("startTime", lowValue);
                    param.putIfAbsent("endTime", 253402271999000L);
                } else {
                    param.putIfAbsent("startTime", 0L);
                    param.putIfAbsent("endTime", lowValue);
                }
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "STEP_DATE_CHAR_GENERATOR");
            } else if ("UNIFORM_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Object lowValue = typeConfig.get("lowValue");
                Object highValue = typeConfig.get("highValue");
                typeConfig.put("lowValue", null);
                typeConfig.put("highValue", null);
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                param.putIfAbsent("start", lowValue);
                param.putIfAbsent("end", highValue);
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "RANDOM_NUMBER_GENERATOR");
            } else if ("FIX_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                param.putIfAbsent("fixText", param.get("fixNum"));
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "FIX_CHAR_GENERATOR");
            } else if ("STEP_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                BigDecimal lowValue = new BigDecimal(typeConfig.get("lowValue").toString());
                typeConfig.put("lowValue", null);
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                Long width = Long.valueOf(typeConfig.get("width").toString());
                Long step = Long.valueOf(param.get("step").toString());
                if (step > 0) {
                    param.putIfAbsent("start", lowValue.toPlainString());
                    if (width <= 19) {
                        param.putIfAbsent("end", new Double(Math.pow(10, width) - 1).longValue());
                    } else {
                        param.putIfAbsent("end", Long.MAX_VALUE);
                    }
                } else {
                    if (width <= 19) {
                        param.putIfAbsent("start", -1 * new Double(Math.pow(10, width - 1) - 1).longValue());
                    } else {
                        param.putIfAbsent("start", Long.MIN_VALUE);
                    }
                    param.putIfAbsent("end", lowValue.toPlainString());
                }
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "STEP_NUMBER_GENERATOR");
            } else if ("NULL_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "NULL_CHAR_GENERATOR");
            }
        } else if ("DATE".equalsIgnoreCase(type)) {
            if ("STEP_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                Long step = Long.valueOf(param.get("step").toString());
                if (step < 0) {
                    Long lowValue = Long.valueOf(typeConfig.get("lowValue").toString());
                    typeConfig.put("lowValue", null);
                    typeConfig.put("highValue", lowValue);
                }
                typeConfig.put("genParams", param);
            } else if ("NULL_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "NULL_DATE_GENERATOR");
            }
        } else if ("TIMESTAMP".equalsIgnoreCase(type)) {
            typeConfig.put("name", "DATE");
            if ("STEP_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                Long step = Long.valueOf(param.get("step").toString());
                if (step < 0) {
                    Long lowValue = Long.valueOf(typeConfig.get("lowValue").toString());
                    typeConfig.put("lowValue", null);
                    typeConfig.put("highValue", lowValue);
                }
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "STEP_TIMESTAMP_GENERATOR");
            } else if ("RANDOM_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "RANDOM_TIMESTAMP_GENERATOR");
            } else if ("FIX_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "FIX_TIMESTAMP_GENERATOR");
            } else if ("NULL_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "NULL_TIMESTAMP_GENERATOR");
            }
        } else if ("DIGIT".equalsIgnoreCase(type)) {
            if ("NULL_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "NULL_DIGIT_GENERATOR");
            } else if ("STEP_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                Long step = Long.valueOf(param.get("step").toString());
                if (step < 0) {
                    Long lowValue = Long.valueOf(typeConfig.get("lowValue").toString());
                    typeConfig.put("lowValue", null);
                    typeConfig.put("highValue", lowValue);
                }
                typeConfig.put("genParams", param);
            }
        } else if ("BYTE".equalsIgnoreCase(type)) {
            typeConfig.put("name", "CHAR");
            if ("RANDOM_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Long lowValue = Long.valueOf(typeConfig.get("lowValue").toString());
                Long highValue = Long.valueOf(typeConfig.get("highValue").toString());
                typeConfig.put("lowValue", null);
                typeConfig.put("highValue", null);
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                param.putIfAbsent("startTime", lowValue);
                param.putIfAbsent("endTime", highValue);
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "RANDOM_DATE_BYTE_GENERATOR");
            } else if ("FIX_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "FIX_DATE_BYTE_GENERATOR");
            } else if ("STEP_DATE_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Long lowValue = Long.valueOf(typeConfig.get("lowValue").toString());
                typeConfig.put("lowValue", null);
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                Long step = Long.valueOf(param.get("step").toString());
                if (step > 0) {
                    param.putIfAbsent("startTime", lowValue);
                    param.putIfAbsent("endTime", 253402271999000L);
                } else {
                    param.putIfAbsent("startTime", 0L);
                    param.putIfAbsent("endTime", lowValue);
                }
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "STEP_DATE_BYTE_GENERATOR");
            } else if ("UNIFORM_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Object lowValue = typeConfig.get("lowValue");
                Object highValue = typeConfig.get("highValue");
                typeConfig.put("lowValue", null);
                typeConfig.put("highValue", null);
                Map<String, Object> param = new HashMap<>();
                param.putIfAbsent("start", lowValue);
                param.putIfAbsent("end", highValue);
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "RANDOM_NUMBER_BYTE_GENERATOR");
            } else if ("FIX_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                param.putIfAbsent("fixText", param.get("fixNum"));
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "FIX_BYTE_GENERATOR");
            } else if ("STEP_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                BigDecimal lowValue = new BigDecimal(typeConfig.get("lowValue").toString());
                typeConfig.put("lowValue", null);
                Map<String, Object> param = (Map<String, Object>) typeConfig.getOrDefault("genParams", new HashMap<>());
                Long width = Long.valueOf(typeConfig.get("width").toString());
                Long step = Long.valueOf(param.get("step").toString());
                if (step > 0) {
                    param.putIfAbsent("start", lowValue.toPlainString());
                    if (width <= 19) {
                        param.putIfAbsent("end", new Double(Math.pow(10, width) - 1).longValue());
                    } else {
                        param.putIfAbsent("end", Long.MAX_VALUE);
                    }
                } else {
                    if (width <= 19) {
                        param.putIfAbsent("start", -1 * new Double(Math.pow(10, width - 1) - 1).longValue());
                    } else {
                        param.putIfAbsent("start", Long.MIN_VALUE);
                    }
                    param.putIfAbsent("end", lowValue.toPlainString());
                }
                typeConfig.put("genParams", param);
                typeConfig.put("generator", "STEP_NUMBER_BYTE_GENERATOR");
            } else if ("NULL_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "NULL_BYTE_GENERATOR");
            } else if ("RANDOM_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "RANDOM_BYTE_GENERATOR");
            } else if ("FIX_CHAR_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "FIX_BYTE_GENERATOR");
            } else if ("BOOL_CHAR_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "BOOL_BYTE_GENERATOR");
            } else if ("REGEXP_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "REGEXP_BYTE_GENERATOR");
            }
        } else if ("INTERVAL".equalsIgnoreCase(type)) {
            typeConfig.put("name", "DATE");
            if ("NULL_GENERATOR".equalsIgnoreCase(typeConfig.get("generator").toString())) {
                typeConfig.put("generator", "NULL_INTERVALYM_GENERATOR");
            }
        }
    }
}
