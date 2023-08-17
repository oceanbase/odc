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
package com.oceanbase.odc.core.migrate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * class level annotation, the annotated class must implement interface {@link JdbcMigratable}
 * 
 * @author yizhou.xw
 * @version : Migratable.java, v 0.1 2021-03-23 16:15
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Migratable {

    /**
     * the version belong to
     */
    String version() default "";

    /**
     * description
     */
    String description() default "";

    /**
     * if the migrate process is repeatable
     */
    boolean repeatable() default false;

    /**
     * if ignore validate checksum
     * 
     * @return
     */
    boolean ignoreChecksum() default false;
}
