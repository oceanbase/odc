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
package com.oceanbase.odc.service.plugin;

import java.util.List;

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.connect.api.ConnectionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.InformationExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.SessionExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.SqlDiagnoseExtensionPoint;
import com.oceanbase.odc.plugin.connect.api.TraceExtensionPoint;

/**
 * @author yaobin
 * @date 2023-04-21
 * @since 4.2.0
 */
public class ConnectionPluginUtil {
    public static OdcPluginManager<DialectType> getOdcPluginManager() {
        PluginService pluginService = new PluginService();
        return pluginService.getConnectionPluginManager();
    }

    public static ConnectionExtensionPoint getConnectionExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, ConnectionExtensionPoint.class);
    }

    public static SessionExtensionPoint getSessionExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, SessionExtensionPoint.class);
    }

    public static TraceExtensionPoint getTraceExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, TraceExtensionPoint.class);
    }

    public static InformationExtensionPoint getInformationExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, InformationExtensionPoint.class);
    }

    public static SqlDiagnoseExtensionPoint getDiagnoseExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, SqlDiagnoseExtensionPoint.class);
    }

    public static <T extends ExtensionPoint> T getSingletonExtension(DialectType dialectType, Class<T> type) {
        return getOdcPluginManager().getSingletonExtension(dialectType, type);
    }

    public static <T extends ExtensionPoint> List<T> getExtensions(DialectType dialectType, Class<T> type) {
        return getOdcPluginManager().getExtensions(dialectType, type);
    }

    public static <T extends ExtensionPoint> List<Class<? extends T>> getExtensionClasses(
            DialectType dialectType, Class<T> type) {
        return getOdcPluginManager().getExtensionClasses(dialectType, type);
    }

}
