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

package com.oceanbase.odc.plugin.task.mysql.datatransfer;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferTask;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectStatus.Status;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.TransferTaskStatus;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.unit.TransferUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MySQLDataTransferTask implements DataTransferTask {
    private final DataTransferConfig baseConfig;
    private final List<TransferUnit> schemaUnits = new LinkedList<>();
    private final List<TransferUnit> dataUnits = new LinkedList<>();
    private volatile int transferUnitNum = 0;

    public MySQLDataTransferTask(DataTransferConfig baseConfig) {
        this.baseConfig = baseConfig;
    }

    @Override
    public TransferTaskStatus transfer() throws Exception {

        initTransferUnits();

        serialHandle();

        return getStatus();

    }

    @Override
    public TransferTaskStatus getStatus() {
        TransferTaskStatus ret = new TransferTaskStatus();
        ret.getDataObjectsInfo().addAll(dataUnits);
        ret.getSchemaObjectsInfo().addAll(schemaUnits);
        return ret;
    }

    @Override
    public double getProgress() {
        if (transferUnitNum == 0) {
            return 0.0;
        }
        int finished = 0;
        finished += dataUnits.stream().filter(ObjectStatus::isDone).count();
        finished += schemaUnits.stream().filter(ObjectStatus::isDone).count();
        return finished * 100D / transferUnitNum;
    }

    private void initTransferUnits() {
        // todo
    }

    private void serialHandle() {
        boolean exit = false;

        /*
         * transfer schema first
         */
        if (CollectionUtils.isNotEmpty(schemaUnits)) {
            for (TransferUnit unit : schemaUnits) {
                try {
                    unit.handle();
                    log.info("Successfully finished transferring schema {} .", unit.getSummary());
                } catch (Exception e) {
                    log.warn("Object {} failed for {}.", unit.getSummary(), e.getMessage());
                    if (baseConfig.isStopWhenError()) {
                        throw e;
                    }
                }
                if (unit.getStatus() == Status.FAILURE && baseConfig.isStopWhenError()) {
                    exit = true;
                    break;
                }
            }
        }
        /*
         * then transfer data. If any error occurred when transferring schema and user has configured
         * {DataTransferConfig#stopWhenError} as true, data transfer will not be executed.
         */
        if (CollectionUtils.isNotEmpty(dataUnits) && !exit) {

            ThroughputReportThread reporter = new ThroughputReportThread();
            new Thread(reporter).start();

            try {
                for (TransferUnit unit : dataUnits) {
                    try {
                        reporter.collect(unit);
                        unit.handle();
                        log.info("Successfully finished transferring data {} .", unit.getSummary());
                    } catch (Exception e) {
                        log.warn("Object {} failed for {}.", unit.getSummary(), e.getMessage());
                        if (baseConfig.isStopWhenError()) {
                            throw e;
                        }
                    }
                    if (unit.getStatus() == Status.FAILURE && baseConfig.isStopWhenError()) {
                        break;
                    }
                }

            } finally {
                reporter.stop();
            }
        }
    }

}
