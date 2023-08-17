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
package com.oceanbase.odc.common.util;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * {@link TraceWatchTest}
 *
 * @author yh263208
 * @date 2022-05-06 20:29
 * @since ODC_release_3.3.1
 */
public class TraceWatchTest {

    @Test
    public void start_noStageStart_emptyStagesReturn() {
        TraceWatch tw = new TraceWatch();
        Assert.assertEquals(0, tw.getStageList().size());
    }

    @Test
    public void test_get() throws IOException {
        try (TraceWatch tw = new TraceWatch()) {
            try (TraceStage stage = tw.start("stage")) {
                try (TraceStage stage1 = tw.start("stage1")) {
                    List<TraceStage> stages = tw.getByTaskName("stage");
                    Assert.assertEquals(stage, stages.get(0));
                }
                // test get subStage
                List<TraceStage> stages = tw.getByTaskName("stage1");
                Assert.assertEquals(1, stages.size());
            }
        }
    }

    @Test
    public void start_startStage_returnNotEmpty() throws IOException {
        try (TraceWatch tw = new TraceWatch()) {
            try (TraceStage ts = tw.start("stage1")) {
                try (TraceStage ts1 = tw.start("stage2")) {

                }
            }
            Assert.assertEquals(1, tw.getStageList().size());
            TraceStage parent = tw.getStageList().get(0);
            Assert.assertEquals("stage1", parent.getMessage());
            Assert.assertEquals("stage2", parent.getSubStageList().get(0).getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void start_traceWatchClosed_expThrown() throws IOException {
        TraceWatch tw = new TraceWatch();
        try (TraceStage ts = tw.start("stage1")) {
            try (TraceStage ts1 = tw.start("stage2")) {

            }
        }
        tw.close();
        tw.start();
    }

    @Test
    public void start_stopStage_returnNotEmpty() {
        TraceWatch tw = new TraceWatch();
        TraceStage ts = tw.start();
        TraceStage ts1 = tw.start("s1");
        tw.stop();
        Assert.assertEquals(0, tw.getStageList().size());
        tw.stop();
        Assert.assertEquals(1, tw.getStageList().size());
    }

    @Test
    public void start_multiThreadsRun_parallelGetTime() throws InterruptedException {
        TraceWatch watch = new TraceWatch();
        Thread t1 = new Thread(() -> {
            watch.start("thread_1");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // eat exp
            }
            watch.start("thread_1_1");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // eat exp
            }
            watch.stop();
            watch.stop();
        });
        Thread t2 = new Thread(() -> {
            watch.start("thread_2");
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // eat exp
            }
            watch.start("thread_2_1");
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                // eat exp
            }
            watch.stop();
            watch.stop();
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assert.assertEquals(2, watch.getStageList().size());
    }

    @Test
    public void suspend_multiThreadsRun_parallelGetTime() throws InterruptedException {
        TraceWatch watch = new TraceWatch();
        Thread t1 = new Thread(() -> {
            watch.start("thread_1");
            watch.start("thread_1_1");
            watch.suspend();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // eat exp
            }
            watch.stop();
            watch.stop();
        });
        Thread t2 = new Thread(() -> {
            watch.start("thread_2");
            watch.suspend();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // eat exp
            }
            watch.resume();
            watch.start("thread_2_1");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // eat exp
            }
            watch.stop();
            watch.stop();
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        Assert.assertEquals(2, watch.getStageList().size());
    }

}
