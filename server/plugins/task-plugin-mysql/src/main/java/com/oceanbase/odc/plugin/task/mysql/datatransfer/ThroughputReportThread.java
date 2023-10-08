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

import com.oceanbase.odc.common.lang.Pair;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.tableformat.DefaultTableFactory;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.unit.TransferUnit;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Async task to monitor and report datatransfer throughput.
 */
@Slf4j
public class ThroughputReportThread implements Runnable {

    private static final List<String> REPORT_HEADER = Arrays.asList("Dimension \\ Metric", "Tps", "Throughput");

    private final AtomicBoolean stopReportControl = new AtomicBoolean(true);
    /**
     * <pre>
     *     [Records, Bytes]
     * </pre>
     */
    private final Pair<Long, Long> runningUnitVolumeContainer = new Pair<>(0L, 0L);
    private final Pair<Long, Long> archivedVolumeContainer = new Pair<>(0L, 0L);
    private final Pair<Long, Long> incrementalVolumeContainer = new Pair<>(0L, 0L);

    private long startTime;
    private long collectTime;
    private long lastCollectTime;
    private TransferUnit now;

    @Override
    public void run() {
        stopReportControl.getAndSet(false);
        startTime = System.currentTimeMillis();
        collectTime = System.currentTimeMillis();

        while (!stopReportControl.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                log.warn("Report thread was interrupted. Will stop report.");
                stop();
                Thread.currentThread().interrupt();
            }

            try {
                if (Objects.nonNull(now)) {
                    collect(now);
                    doReport();
                }
            } catch (Exception ignore) {
                // eat exception
            }
        }
        collect(now);
        doReport();
        stop();
    }

    public void stop() {
        stopReportControl.getAndSet(true);
    }

    public synchronized void collect(@NonNull TransferUnit next) {
        if (now == null) {
            now = next;
        }

        lastCollectTime = collectTime;
        collectTime = System.currentTimeMillis();

        incrementalVolumeContainer.left = now.getRecords() - runningUnitVolumeContainer.left;
        incrementalVolumeContainer.right = now.getBytes() - runningUnitVolumeContainer.right;

        if (!Objects.equals(next, now)) {
            if (!now.isDone()) {
                log.warn("Object {} will stop report status but it has not finished yet.", now.getSummary());
            }
            /*
             * current unit will be replaced, clear running container.
             */
            incrementalVolumeContainer.left += next.getRecords();
            incrementalVolumeContainer.right += next.getBytes();
            now = next;
        }

        runningUnitVolumeContainer.left = now.getRecords();
        runningUnitVolumeContainer.right = now.getBytes();
        archivedVolumeContainer.left += incrementalVolumeContainer.left;
        archivedVolumeContainer.right += incrementalVolumeContainer.right;
    }

    private void doReport() {
        List<String> reportContent = new ArrayList<>();

        reportContent.add("Real-time");
        double duration = (collectTime - lastCollectTime) * 1D / 1000D;
        Double realTimeRecordSpeed = incrementalVolumeContainer.left / duration;
        Long realTimeByteSpeed = (long) (incrementalVolumeContainer.right / duration);
        reportContent.add(realTimeRecordSpeed + " Records/sec");
        reportContent.add(BinarySizeUnit.B.of(realTimeByteSpeed) + "/sec");

        reportContent.add("Average");
        double elapsed = (collectTime - startTime) * 1D / 1000;
        Double averageRecordSpeed = archivedVolumeContainer.left / elapsed;
        Long averageByteSpeed = (long) (archivedVolumeContainer.right / elapsed);
        reportContent.add(averageRecordSpeed + " Records/sec");
        reportContent.add(BinarySizeUnit.B.of(averageByteSpeed) + "/sec");

        reportContent.add("Total");
        reportContent.add(archivedVolumeContainer.left + " Records");
        reportContent.add(BinarySizeUnit.B.of(archivedVolumeContainer.right).toString());

        log.info("Throughput report:\n" + createReportTable(reportContent).render());
    }

    private Table createReportTable(List<String> reportContent) {
        return new DefaultTableFactory().generateTable(3, REPORT_HEADER, reportContent);
    }
}
