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

import org.pf4j.ExtensionPoint;

import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferExtensionPoint;
import com.oceanbase.odc.plugin.task.api.partitionplan.AutoPartitionExtensionPoint;

public class TaskPluginUtil {

    public static OdcPluginManager<DialectType> getOdcPluginManager() {
        PluginService pluginService = new PluginService();
        return pluginService.getTaskPluginManager();
    }

    public static DataTransferExtensionPoint getDataTransferExtension(DialectType dialectType) {
        return getSingletonExtension(dialectType, DataTransferExtensionPoint.class);
    }

    public static AutoPartitionExtensionPoint getAutoPartitionExtensionPoint(DialectType dialectType) {
        return getSingletonExtension(dialectType, AutoPartitionExtensionPoint.class);
    }

    public static <T extends ExtensionPoint> T getSingletonExtension(DialectType dialectType, Class<T> type) {
        return getOdcPluginManager().getSingletonExtension(dialectType, type);
    }

}
