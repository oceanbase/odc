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

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.common.util.tableformat.BorderStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.AbbreviationStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.HorizontalAlign;
import com.oceanbase.odc.common.util.tableformat.CellStyle.NullStyle;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.plugin.task.api.datatransfer.DataTransferJob;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferTaskResult;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.DataSourceManager;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.TransferJobFactory;
import com.oceanbase.tools.loaddump.common.model.ObjectStatus.Status;

import lombok.NonNull;

public class MySQLDataTransferJob implements DataTransferJob {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");
    private static final List<String> REPORT_HEADER = Arrays.asList("No.#", "Type", "Name", "Count", "Status");

    private final DataTransferConfig baseConfig;
    private final File workingDir;
    private final File logDir;
    private final List<URL> inputs;
    private final AtomicInteger finishedJobNum = new AtomicInteger(0);
    private final List<AbstractJob> schemaJobs = new LinkedList<>();
    private final List<AbstractJob> dataJobs = new LinkedList<>();

    private volatile AtomicReference<TaskStatus> status;
    private int transferJobNum = 0;

    public MySQLDataTransferJob(@NonNull DataTransferConfig config, @NonNull File workingDir, @NonNull File logDir,
            @NonNull List<URL> inputs) {
        this.baseConfig = config;
        this.workingDir = workingDir;
        this.logDir = logDir;
        this.inputs = inputs;
        initTransferJobs();
    }

    @Override
    public List<ObjectResult> getDataObjectsStatus() {
        return dataJobs.stream().map(AbstractJob::getObject).collect(Collectors.toList());
    }

    @Override
    public List<ObjectResult> getSchemaObjectsStatus() {
        return schemaJobs.stream().map(AbstractJob::getObject).collect(Collectors.toList());
    }

    @Override
    public double getProgress() {
        if (transferJobNum == 0) {
            return 0.0;
        }
        return finishedJobNum.get() * 100D / transferJobNum;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        TaskStatus newStatus = status.getAndSet(TaskStatus.CANCELED);
        return newStatus == TaskStatus.CANCELED;
    }

    @Override
    public boolean isCanceled() {
        return status.get() == TaskStatus.CANCELED;
    }

    @Override
    public DataTransferTaskResult call() throws Exception {

        if (CollectionUtils.isNotEmpty(schemaJobs)) {
            try {
                runSchemaJobs();
            } finally {
                logSummary(schemaJobs, "SCHEMA");
                DataSourceManager.getInstance().revoke(baseConfig.getConnectionInfo());
            }
        }

        if (CollectionUtils.isNotEmpty(dataJobs) && !isCanceled()) {
            try {
                runDataJobs();
            } finally {
                logSummary(dataJobs, "DATA");
                DataSourceManager.getInstance().revoke(baseConfig.getConnectionInfo());
            }
        }

        return new DataTransferTaskResult(getDataObjectsStatus(), getSchemaObjectsStatus());
    }

    private void initTransferJobs() {
        TransferJobFactory factory = new TransferJobFactory(baseConfig, workingDir, inputs);
        try {
            if (baseConfig.isTransferDDL()) {
                List<AbstractJob> jobs = factory.generateSchemaTransferJobs();
                if (CollectionUtils.isNotEmpty(jobs)) {
                    schemaJobs.addAll(jobs);
                    transferJobNum += jobs.size();
                }
                LOGGER.info("Found {} schema jobs for database {}.", jobs.size(), baseConfig.getSchemaName());
            }
            if (baseConfig.isTransferData()) {
                List<AbstractJob> jobs = factory.generateDataTransferJobs();
                if (CollectionUtils.isNotEmpty(jobs)) {
                    dataJobs.addAll(jobs);
                    transferJobNum += jobs.size();
                }
                LOGGER.info("Found {} data jobs for database {}.", jobs.size(), baseConfig.getSchemaName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to init transfer jobs.", e);
        }
    }

    private void runSchemaJobs() {
        if (CollectionUtils.isEmpty(schemaJobs)) {
            return;
        }
        for (AbstractJob job : schemaJobs) {
            if (job.getObject().getStatus() == Status.SUCCESS || job.getObject().getStatus() == Status.FAILURE) {
                continue;
            }
            try {
                LOGGER.info("Begin to transfer schema for {}.", job);
                job.run();

                finishedJobNum.getAndIncrement();
                LOGGER.info("Successfully finished transferring schema for {}.", job);
            } catch (Exception e) {
                LOGGER.warn("Object {} failed.", job, e);
                job.getObject().setStatus(Status.FAILURE);
            }
            if (job.getObject().getStatus() == Status.FAILURE && baseConfig.isStopWhenError()) {
                throw new RuntimeException(
                        String.format("Object %s failed, transferring will stop.", job));
            }
        }
    }

    private void runDataJobs() {
        if (CollectionUtils.isEmpty(dataJobs)) {
            return;
        }
        for (AbstractJob job : dataJobs) {
            if (job.getObject().getStatus() == Status.SUCCESS || job.getObject().getStatus() == Status.FAILURE) {
                continue;
            }
            try {
                LOGGER.info("Begin to transfer data for {}.", job);
                job.run();

                finishedJobNum.getAndIncrement();
                LOGGER.info("Successfully finished transferring data for {} .", job);
            } catch (Exception e) {
                LOGGER.warn("Object {} failed.", job, e);
                job.getObject().setStatus(Status.FAILURE);
            }
            if (job.getObject().getStatus() == Status.FAILURE && baseConfig.isStopWhenError()) {
                throw new RuntimeException(
                        String.format("Object %s failed, transferring will stop.", job));
            }
        }
    }

    private void logSummary(List<AbstractJob> jobs, String scope) {
        List<String> summary = new ArrayList<>();
        int index = 1;
        for (AbstractJob job : jobs) {
            summary.add(index++ + "");
            summary.add(job.getObject().getType());
            summary.add(job.getObject().getName());
            summary.add(job.getObject().getCount() == null ? "" : job.getObject().getCount().toString());
            summary.add(job.getObject().getStatus().name());
        }

        Table table = new Table(5, BorderStyle.HORIZONTAL_ONLY);
        for (int i = 0; i < 5; i++) {
            table.setColumnWidth(i, 10, 30);
        }
        // set header
        setCell(table, REPORT_HEADER);
        // set body
        setCell(table, summary);
        LOGGER.info("Transfer summary for {} job:\n{}", scope, table.render());

    }

    private void setCell(Table table, List<String> rowContent) {
        CellStyle cs = new CellStyle(HorizontalAlign.CENTER, AbbreviationStyle.DOTS, NullStyle.NULL_TEXT);
        rowContent.forEach(h -> table.addCell(h != null ? h : "", cs));
    }
}
