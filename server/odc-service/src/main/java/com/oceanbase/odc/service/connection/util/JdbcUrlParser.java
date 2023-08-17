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
package com.oceanbase.odc.service.connection.util;

import java.util.List;
import java.util.Map;

import com.oceanbase.odc.service.connection.model.HostAddress;

/**
 * {@link JdbcUrlParser}
 *
 * @author yh263208
 * @date 2022-09-29
 * @since ODC_release_3.5.0
 */
public interface JdbcUrlParser {
    /**
     * get list of {@link HostAddress} eg. {@code jdbc:oceanbase://0.0.0.0:1231,8.8.8.8:1234} this jdbc
     * url contains two {@link HostAddress}, as follow:
     * 
     * <pre>
     *     0.0.0.0:1231
     *     8.8.8.8:1234
     * </pre>
     *
     * @return list of {@link HostAddress}
     */
    List<HostAddress> getHostAddresses();

    /**
     * the schema you want to connect
     *
     * @return schema string
     */
    String getSchema();

    /**
     * get list of parameter
     *
     * @return url parameter
     */
    Map<String, Object> getParameters();

}
