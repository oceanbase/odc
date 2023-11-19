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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.tableformat.BorderStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.AbbreviationStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.HorizontalAlign;
import com.oceanbase.odc.common.util.tableformat.CellStyle.NullStyle;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.core.shared.constant.TaskStatus;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;

import lombok.NonNull;

/**
 * Async task to monitor and report datatransfer throughput.
 */
public class ThroughputMonitor implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");
    private static final List<String> REPORT_HEADER = Arrays.asList("Dimension \\ Metric", "Tps", "Throughput");

    private final AtomicBoolean stopReportControl = new AtomicBoolean(true);
    private final MySQLDataTransferJob job;
    private final Long maxExportSizeBytes;
    /**
     * [Records, Bytes]
     */
    private final Pair<Long, Long> runningJobVolumeContainer = new Pair<>(0L, 0L);
    private final Pair<Long, Long> archivedVolumeContainer = new Pair<>(0L, 0L);
    private final Pair<Long, Long> incrementalVolumeContainer = new Pair<>(0L, 0L);

    private long startTime;
    private long collectTime;
    private long lastCollectTime;
    private long lastReportTime;
    private AbstractJob current;

    public ThroughputMonitor(MySQLDataTransferJob job, Long maxExportSizeBytes) {
        this.job = job;
        this.maxExportSizeBytes = maxExportSizeBytes;
    }

    @Override
    public void run() {
        stopReportControl.getAndSet(false);
        startTime = System.currentTimeMillis();
        collectTime = System.currentTimeMillis();
        lastReportTime = System.currentTimeMillis();

        while (!stopReportControl.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
                if (Objects.isNull(current)) {
                    continue;
                }
                collect();
                doReport();
                cancelJobOnMaxExportSize();
            } catch (InterruptedException e) {
                LOGGER.warn("Report thread was interrupted. Will stop report.");
                stop();
                return;
            } catch (Exception ignore) {
                // eat exception
            }
        }
    }

    public void stop() {
        stopReportControl.getAndSet(true);
    }

    public synchronized void collect() {
        collect(current);
    }

    public synchronized void collect(@NonNull AbstractJob next) {
        if (stopReportControl.get()) {
            return;
        }
        if (current == null) {
            current = next;
        }

        lastCollectTime = collectTime;
        collectTime = System.currentTimeMillis();

        incrementalVolumeContainer.left = current.getRecords() - runningJobVolumeContainer.left;
        incrementalVolumeContainer.right = current.getBytes() - runningJobVolumeContainer.right;

        if (!Objects.equals(next, current)) {
            if (!current.isDone()) {
                LOGGER.warn("Object {} will stop report status but it has not finished yet.", current);
            }
            /*
             * current unit will be replaced, clear running container.
             */
            incrementalVolumeContainer.left += next.getRecords();
            incrementalVolumeContainer.right += next.getBytes();
            current = next;
        }

        runningJobVolumeContainer.left = current.getRecords();
        runningJobVolumeContainer.right = current.getBytes();
        archivedVolumeContainer.left += incrementalVolumeContainer.left;
        archivedVolumeContainer.right += incrementalVolumeContainer.right;
    }

    private void cancelJobOnMaxExportSize() {
        if (maxExportSizeBytes == null || archivedVolumeContainer.right < maxExportSizeBytes) {
            return;
        }
        LOGGER.info("Total size {} exceeds limit {}, stop exporting {} right now.",
                archivedVolumeContainer.right, maxExportSizeBytes, current);
        current.cancel();
        job.mark(TaskStatus.DONE);
    }

    private void doReport() {
        if (stopReportControl.get() || System.currentTimeMillis() - lastReportTime < 5000) {
            return;
        }
        List<String> reportContent = new ArrayList<>();

        reportContent.add("Real-time");
        double duration = (collectTime - lastCollectTime) * 1D / 1000;
        Double realTimeRecordSpeed = incrementalVolumeContainer.left / duration;
        long realTimeByteSpeed = (long) (incrementalVolumeContainer.right / duration);
        reportContent.add(String.format("%.2f", realTimeRecordSpeed) + " Records/sec");
        reportContent.add(BinarySizeUnit.B.of(realTimeByteSpeed) + "/sec");

        reportContent.add("Average");
        double elapsed = (collectTime - startTime) * 1D / 1000;
        Double averageRecordSpeed = archivedVolumeContainer.left / elapsed;
        long averageByteSpeed = (long) (archivedVolumeContainer.right / elapsed);
        reportContent.add(String.format("%.2f", averageRecordSpeed) + " Records/sec");
        reportContent.add(BinarySizeUnit.B.of(averageByteSpeed) + "/sec");

        reportContent.add("Total");
        reportContent.add(archivedVolumeContainer.left + " Records");
        reportContent.add(BinarySizeUnit.B.of(archivedVolumeContainer.right).toString());

        LOGGER.info("Throughput report:\n" + createReportTable(reportContent).render());
    }

    private Table createReportTable(List<String> reportContent) {
        Table table = new Table(3, BorderStyle.HORIZONTAL_ONLY);
        for (int i = 0; i < 3; i++) {
            table.setColumnWidth(i, 10, 30);
        }
        // set header
        setCell(table, REPORT_HEADER);
        // set body
        setCell(table, reportContent);
        return table;
    }

    private void setCell(Table table, List<String> rowContent) {
        CellStyle cs = new CellStyle(HorizontalAlign.CENTER, AbbreviationStyle.DOTS, NullStyle.NULL_TEXT);
        rowContent.forEach(h -> table.addCell(h != null ? h : "", cs));
    }

}
