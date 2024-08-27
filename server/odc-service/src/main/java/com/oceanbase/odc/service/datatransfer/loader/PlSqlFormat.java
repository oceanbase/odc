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
package com.oceanbase.odc.service.datatransfer.loader;

import java.util.HashSet;
import java.util.Set;

import com.oceanbase.odc.common.util.StringUtils;

/**
 * @author youshu
 * @date 2024/8/27
 */
public class PlSqlFormat {
    private static final Set<String>     LEGAL_FILE_SUFFIXES = new HashSet<>();
    private static final Set<ObjectType> PL_OBJECTS          = new HashSet<>();

    static {
        for (ObjectType type : ObjectType.values()) {
            LEGAL_FILE_SUFFIXES.add(type.suffix);
        }
        PL_OBJECTS.add(ObjectType.FUNCTION);
        PL_OBJECTS.add(ObjectType.PROCEDURE);
        PL_OBJECTS.add(ObjectType.TRIGGER);
        PL_OBJECTS.add(ObjectType.TYPE);
        PL_OBJECTS.add(ObjectType.PACKAGE);
        PL_OBJECTS.add(ObjectType.TYPE_BODY);
        PL_OBJECTS.add(ObjectType.PACKAGE_BODY);
    }

    public enum ObjectType {
        TABLE("tab"),
        VIEW("vw"),
        SEQUENCE("seq"),
        SYNONYM("syn"),
        // pl types
        PROCEDURE("prc"),
        FUNCTION("fnc"),
        TYPE("tps"),
        TRIGGER("trg"),
        PACKAGE("spc"),
        PACKAGE_BODY("bdy"),
        TYPE_BODY("tpb"),
        // unknown types
        UNKNOWN("null");

        private final String suffix;

        ObjectType(String suffix) {
            this.suffix = suffix;
        }

        static ObjectType from(String suffix) {
            for (ObjectType type : ObjectType.values()) {
                if (StringUtils.equals(suffix, type.suffix)) {
                    return type;
                }
            }
            return UNKNOWN;
        }

        boolean isPlObject() {
            return PL_OBJECTS.contains(this);
        }
    }

    public static boolean isPlFileSuffix(String suffix) {
        return PlSqlFormat.LEGAL_FILE_SUFFIXES.contains(suffix);
    }

}
