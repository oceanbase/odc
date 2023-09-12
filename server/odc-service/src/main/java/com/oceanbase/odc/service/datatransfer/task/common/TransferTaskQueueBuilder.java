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

package com.oceanbase.odc.service.datatransfer.task.common;

import static com.oceanbase.odc.service.datatransfer.model.DataTransferFormat.CSV;
import static com.oceanbase.odc.service.datatransfer.model.DataTransferFormat.EXCEL;
import static com.oceanbase.odc.service.datatransfer.model.DataTransferFormat.SQL;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.service.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.service.datatransfer.task.TransferTask;
import com.oceanbase.odc.service.datatransfer.task.TransferTaskFactory;
import com.oceanbase.odc.service.datatransfer.task.datax.DataXTaskFactory;
import com.oceanbase.odc.service.datatransfer.task.obloaderdumper.ObLoaderDumperTaskFactory;
import com.oceanbase.odc.service.datatransfer.task.sql.SqlTaskFactory;

public class TransferTaskQueueBuilder {

    public static List<TransferTask> build(DataTransferConfig config) {

        return getFactories(config).stream()
                .map(TransferTaskFactory::generate)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static List<TransferTaskFactory> getFactories(DataTransferConfig config) {
        ConnectType connectType = config.getConnectionConfig().getType();
        /*
         * OceanBase connection
         */
        if (connectType.isOceanBase()) {
            return Collections.singletonList(new ObLoaderDumperTaskFactory(config));
        }
        /*
         * Any other, MySQL e.g.
         */
        List<TransferTaskFactory> factories = new LinkedList<>();
        if (config.isTransferDDL() || config.getDataTransferFormat() == SQL) {
            factories.add(new SqlTaskFactory(config));
        }
        if (config.isTransferData()
                && (config.getDataTransferFormat() == CSV || config.getDataTransferFormat() == EXCEL)) {
            factories.add(new DataXTaskFactory(config));
        }
        return factories;
    }

}
