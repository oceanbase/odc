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
package com.oceanbase.odc.core.datasource;

import java.sql.Driver;

import lombok.NonNull;

/**
 * Use classname for database-driven data source loading. Corresponding to this, heterogeneous
 * driver loading can also be implemented in the form of a {@link ClassLoader}, but it has not yet
 * been implemented.
 *
 * @author yh263208
 * @date 2021-11-10 15:45
 * @since ODC_release_3.2.2
 * @see BaseDriverBasedDataSource
 */
public abstract class BaseClassBasedDataSource extends BaseDriverBasedDataSource {
    /**
     * Default class name
     */
    public static final String DEFAULT_DRIVER_CLASS_NAME = "com.oceanbase.jdbc.Driver";
    /**
     * The default driver class name is the driver of oceanbase
     */
    private String driverClassName = DEFAULT_DRIVER_CLASS_NAME;

    public void setDriverClassName(@NonNull String driverClassName) {
        try {
            Class.forName(driverClassName);
            this.driverClassName = driverClassName;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected Driver getDriver() throws ClassNotFoundException {
        Class<?> clazz = Class.forName(this.driverClassName);
        try {
            return (Driver) clazz.newInstance();
        } catch (Exception e) {
            throw new InternalError(e);
        }
    }

}

