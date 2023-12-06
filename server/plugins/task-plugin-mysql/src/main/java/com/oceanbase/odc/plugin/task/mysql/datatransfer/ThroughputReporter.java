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
/*
 * Copybytes (c) 2023 OceanBase.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oceanbase.odc.common.event.AbstractEventListener;
import com.oceanbase.odc.common.unit.BinarySizeUnit;
import com.oceanbase.odc.common.util.tableformat.BorderStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.AbbreviationStyle;
import com.oceanbase.odc.common.util.tableformat.CellStyle.HorizontalAlign;
import com.oceanbase.odc.common.util.tableformat.CellStyle.NullStyle;
import com.oceanbase.odc.common.util.tableformat.Table;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.MySQLDataTransferJob.ObjectStartEvent;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.AbstractJob;

import lombok.NonNull;

/**
 * Async task to monitor and report datatransfer throughput.
 *
 * @author liuyizhuo.lyz
 * @date 2023/12/04
 */
public class ThroughputReporter extends AbstractEventListener<ObjectStartEvent> implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");
    private static final List<String> REPORT_HEADER = Arrays.asList("Dimension \\ Metric", "Tps", "Throughput");

    private final StatisticsContainer runningContainer = new StatisticsContainer(0, 0);
    private final StatisticsContainer archivedContainer = new StatisticsContainer(0, 0);
    private final StatisticsContainer incrementalContainer = new StatisticsContainer(0, 0);

    private long startTime = System.currentTimeMillis();
    private long collectTime = System.currentTimeMillis();
    private long lastCollectTime;
    private AbstractJob current;

    @Override
    public void onEvent(ObjectStartEvent event) {
        collect((AbstractJob) event.getSource());
    }

    @Override
    public void run() {
        try {
            if (Objects.isNull(current)) {
                return;
            }
            collect(current);
            doReport();
        } catch (Exception ignore) {
            // eat exception
        }
    }

    public synchronized void collect(@NonNull AbstractJob next) {
        if (current == null) {
            current = next;
        }

        lastCollectTime = collectTime;
        collectTime = System.currentTimeMillis();

        incrementalContainer.records = current.getRecords() - runningContainer.records;
        incrementalContainer.bytes = current.getBytes() - runningContainer.bytes;

        if (!Objects.equals(next, current)) {
            if (!current.isDone()) {
                LOGGER.warn("Object {} will stop report status but it has not finished yet.", current);
            }
            /*
             * current unit will be replaced, clear running container.
             */
            incrementalContainer.records += next.getRecords();
            incrementalContainer.bytes += next.getBytes();
            current = next;
        }

        runningContainer.records = current.getRecords();
        runningContainer.bytes = current.getBytes();
        archivedContainer.records += incrementalContainer.records;
        archivedContainer.bytes += incrementalContainer.bytes;
    }

    private void doReport() {
        List<String> reportContent = new ArrayList<>();

        reportContent.add("Real-time");
        double duration = (collectTime - lastCollectTime) * 1D / 1000;
        Double realTimeRecordSpeed = incrementalContainer.records / duration;
        long realTimeByteSpeed = (long) (incrementalContainer.bytes / duration);
        reportContent.add(String.format("%.2f", realTimeRecordSpeed) + " Records/sec");
        reportContent.add(BinarySizeUnit.B.of(realTimeByteSpeed) + "/sec");

        reportContent.add("Average");
        double elapsed = (collectTime - startTime) * 1D / 1000;
        Double averageRecordSpeed = archivedContainer.records / elapsed;
        long averageByteSpeed = (long) (archivedContainer.bytes / elapsed);
        reportContent.add(String.format("%.2f", averageRecordSpeed) + " Records/sec");
        reportContent.add(BinarySizeUnit.B.of(averageByteSpeed) + "/sec");

        reportContent.add("Total");
        reportContent.add(archivedContainer.records + " Records");
        reportContent.add(BinarySizeUnit.B.of(archivedContainer.bytes).toString());

        Table table = new Table(3, BorderStyle.HORIZONTAL_ONLY);
        for (int i = 0; i < 3; i++) {
            table.setColumnWidth(i, 15, 30);
        }
        setCell(table, REPORT_HEADER);
        setCell(table, reportContent);
        LOGGER.info("Throughput report:\n" + table.render() + "\n");
    }

    private void setCell(Table table, List<String> rowContent) {
        CellStyle cs = new CellStyle(HorizontalAlign.CENTER, AbbreviationStyle.DOTS, NullStyle.NULL_TEXT);
        rowContent.forEach(h -> table.addCell(h != null ? h : "", cs));
    }

    private static class StatisticsContainer {
        private long records;
        private long bytes;

        public StatisticsContainer(long records, long bytes) {
            this.records = records;
            this.bytes = bytes;
        }
    }
}
