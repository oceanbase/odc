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
package com.oceanbase.odc.service.flow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.oceanbase.odc.service.common.FileManager;
import com.oceanbase.odc.service.common.model.FileBucket;
import com.oceanbase.odc.service.common.model.FileMeta;
import com.oceanbase.odc.service.flow.task.DatabaseChangeThread;
import com.oceanbase.odc.service.session.model.SqlExecuteResult;

public class DatabaseChangeThreadTest {
    @Test
    public void writeJsonFile() {
        List<SqlExecuteResult> executeResult = new ArrayList<>();
        executeResult.add(new SqlExecuteResult());
        String fileDir = FileManager.generateDir(FileBucket.ASYNC);

        String jsonFileName = DatabaseChangeThread.writeJsonFile(fileDir, executeResult);
        File file = new File(String.format("%s/%s.json", fileDir, jsonFileName));
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.delete());
    }

    @Test
    public void writeZipFile() {
        String fileDir = FileManager.generateDir(FileBucket.ASYNC);
        List<SqlExecuteResult> executeResult = new ArrayList<>();
        executeResult.add(new SqlExecuteResult());
        long flowInstanceId = 123L;
        FileMeta fileMeta = DatabaseChangeThread.writeZipFile(fileDir, executeResult, null, flowInstanceId);
        String zipFileName = fileMeta.getFileId() + ".zip";
        File file = new File(String.format("%s/%s", fileDir, zipFileName));
        Assert.assertTrue(file.exists());
        Assert.assertTrue(file.delete());
    }
}
