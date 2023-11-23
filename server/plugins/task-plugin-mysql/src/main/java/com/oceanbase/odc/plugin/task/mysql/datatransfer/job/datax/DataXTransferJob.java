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

package com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.datax.common.element.ColumnCast;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.common.util.MessageSource;
import com.alibaba.datax.core.job.JobContainer;
import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.CommunicationTool;
import com.alibaba.datax.core.statistics.container.communicator.job.StandAloneJobContainerCommunicator;
import com.alibaba.datax.core.util.container.CoreConstant;
import com.alibaba.datax.core.util.container.LoadUtil;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobConfiguration;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.TxtWriterPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.util.DataXJobIdProvider;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

public class DataXTransferJob extends AbstractJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    private final JobConfiguration jobConfig;
    /**
     * for monitoring, {@link StandAloneJobContainerCommunicator} need this to collect. Each job should
     * own a unique ID.
     */
    private final Long jobId;
    private JobContainer jobContainer;
    private StandAloneJobContainerCommunicator containerCommunicator;

    public DataXTransferJob(ObjectResult object, JobConfiguration jobConfig) {
        super(object);
        this.jobConfig = jobConfig;
        this.jobId = DataXJobIdProvider.getInstance().fetch();
    }

    @Override
    public void run() throws Exception {

        DataXJobMonitor monitor = new DataXJobMonitor();

        try {
            Configuration configuration = ConfigurationResolver.resolve(jobConfig);
            configuration.set(CoreConstant.DATAX_CORE_CONTAINER_JOB_ID, jobId);
            this.containerCommunicator = new StandAloneJobContainerCommunicator(configuration);

            new Thread(monitor, "DataX-Monitor-" + Thread.currentThread().getName()).start();

            // bind column cast strategies
            ColumnCast.bind(configuration);
            // initiate PluginLoader
            LoadUtil.bind(configuration);
            // bind i18n properties
            MessageSource.init(
                    Configuration.from(DataXTransferJob.class.getResourceAsStream("/datax/conf/core.json")));

            this.jobContainer = new JobContainer(configuration);
            jobContainer.start();

            setTaskStatus();

            if (StringUtils.equalsIgnoreCase(jobConfig.getContent()[0].getWriter().getName(),
                    Constants.TXT_FILE_WRITER)) {
                setExportFile();
            }

        } catch (Throwable e) {
            setStatus(Status.FAILURE);
            getCommunicationAndRecord();
            throw e;

        } finally {
            monitor.stop();
        }
    }

    @Override
    public void cancel() {
        if (jobContainer != null) {
            jobContainer.cancel();
        }
        super.cancel();
    }

    private void setTaskStatus() {
        Communication communication = getCommunicationAndRecord();

        if (communication.getThrowable() != null) {
            /*
             * print warning only without throwing
             */
            LOGGER.warn("DataX task finished with exception: {}", communication.getThrowable().getMessage());
        }

        if (CommunicationTool.getTotalErrorRecords(communication) == 0) {
            setStatus(Status.SUCCESS);
        } else {
            setStatus(Status.FAILURE);
            throw new RuntimeException(String.format("DataX task finished with some failed records. "
                    + "Number of readFailedRecords: %d, number of writeFailedRecords: %d",
                    communication.getLongCounter("readFailedRecords"),
                    communication.getLongCounter("writeFailedRecords")));
        }
    }

    private Communication getCommunicationAndRecord() {
        if (containerCommunicator == null) {
            return new Communication();
        }
        Communication communication = containerCommunicator.collect();
        communication.setTimestamp(System.currentTimeMillis());

        increaseTotal(CommunicationTool.getTotalReadRecords(communication));
        increaseCount(CommunicationTool.getWriteSucceedRecords(communication));
        return communication;
    }

    private void setExportFile() throws MalformedURLException, FileNotFoundException {
        TxtWriterPluginParameter pluginParameter =
                (TxtWriterPluginParameter) jobConfig.getContent()[0].getWriter().getParameter();
        File file = Paths.get(pluginParameter.getPath(), pluginParameter.getFileName()).toFile();
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException(file.getName());
        }
        object.setExportPaths(Collections.singletonList(file.toURI().toURL()));
    }

    private class DataXJobMonitor implements Runnable {
        private final AtomicBoolean stop = new AtomicBoolean();

        @Override
        public void run() {
            while (!(isCanceled() || stop.get() || Thread.currentThread().isInterrupted())) {
                try {
                    Communication communication = getCommunicationAndRecord();
                    bytes += communication.getLongCounter(CommunicationTool.REAL_WRITE_BYTES);
                    records += CommunicationTool.getTotalReadRecords(communication);

                    Thread.sleep(1000);
                } catch (Exception e) {
                    LOGGER.warn("Error occurred on dataX monitoring, reason:{}. Transfer will continue.",
                            e.getMessage());
                }
            }

            /*
             * collect for the last time
             */
            Communication communication = getCommunicationAndRecord();
            bytes += communication.getLongCounter(CommunicationTool.REAL_WRITE_BYTES);
            records += CommunicationTool.getTotalReadRecords(communication);
        }

        public void stop() {
            stop.getAndSet(true);
        }

    }

}
