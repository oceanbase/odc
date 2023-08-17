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
package com.oceanbase.odc.service.datatransfer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.oceanbase.odc.service.datatransfer.dumper.AbstractOutputFile;
import com.oceanbase.odc.service.datatransfer.dumper.DumpDBObject;
import com.oceanbase.odc.service.datatransfer.dumper.DumperOutput;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;

/**
 * Test cases for {@link DumperOutput}
 *
 * @author yh263208
 * @date 2022-07-01 10:25
 * @since ODC_release_3.4.0
 */
public class DumperOutputTest {

    @Test
    public void getManifest_fromZipFile_returnNotNull() throws IOException {
        DumperOutput output = new DumperOutput(getDumpZip());
        Assert.assertNotNull(output.getManifest());
    }

    @Test
    public void getManifest_fromFolder_returnNotNull() throws IOException {
        DumperOutput output = new DumperOutput(getDumpFolder());
        Assert.assertNotNull(output.getManifest());
    }

    @Ignore
    @Test
    public void getCheckPoints_fromZipFile_returnNotNull() throws IOException {
        DumperOutput output = new DumperOutput(getDumpZip());

        Assert.assertNotNull(output.getCheckpoints());
        Assert.assertEquals(1, output.getCheckpoints().getTarget().size());
    }

    @Ignore
    @Test
    public void getCheckPoints_fromFolder_returnNotNull() throws IOException {
        DumperOutput output = new DumperOutput(getDumpFolder());

        Assert.assertNotNull(output.getCheckpoints());
        Assert.assertEquals(1, output.getCheckpoints().getTarget().size());
    }

    @Test
    public void getObjectFolders_fromZipFile_tableObjectReturn() throws IOException {
        DumperOutput output = new DumperOutput(getDumpZip());
        List<DumpDBObject> objectFolderList = output.getDumpDbObjects();

        Assert.assertEquals(1, objectFolderList.size());
        DumpDBObject folder = objectFolderList.get(0);
        Assert.assertEquals(ObjectType.TABLE, folder.getObjectType());
    }

    @Test
    public void getObjectFolders_fromFolder_tableObjectReturn() throws IOException {
        DumperOutput output = new DumperOutput(getDumpFolder());
        List<DumpDBObject> objectFolderList = output.getDumpDbObjects();

        Assert.assertEquals(1, objectFolderList.size());
        DumpDBObject folder = objectFolderList.get(0);
        Assert.assertEquals(ObjectType.TABLE, folder.getObjectType());
    }

    @Test
    public void getOutputFiles_fromZipFile_2OutputFileReturn() throws IOException {
        DumperOutput output = new DumperOutput(getDumpZip());
        List<DumpDBObject> objectFolderList = output.getDumpDbObjects();
        DumpDBObject folder = objectFolderList.get(0);
        List<AbstractOutputFile> outputFiles = folder.getOutputFiles();

        Assert.assertEquals(2, outputFiles.size());
        for (AbstractOutputFile item : outputFiles) {
            Assert.assertEquals("TEST", item.getObjectName());
            Assert.assertEquals(ObjectType.TABLE, item.getObjectType());
        }
    }

    @Test
    public void getOutputFiles_fromFolder_2OutputFileReturn() throws IOException {
        DumperOutput output = new DumperOutput(getDumpFolder());
        List<DumpDBObject> objectFolderList = output.getDumpDbObjects();
        DumpDBObject folder = objectFolderList.get(0);
        List<AbstractOutputFile> outputFiles = folder.getOutputFiles();

        Assert.assertEquals(2, outputFiles.size());
        for (AbstractOutputFile item : outputFiles) {
            Assert.assertEquals("TEST", item.getObjectName());
            Assert.assertEquals(ObjectType.TABLE, item.getObjectType());
        }
    }

    private File getDumpFolder() {
        URL url = DumperOutput.class.getClassLoader().getResource("datatransfer/data");
        assert url != null;
        return new File(url.getPath());
    }

    private File getDumpZip() {
        URL url = DumperOutput.class.getClassLoader().getResource("datatransfer/export.zip");
        assert url != null;
        return new File(url.getPath());
    }
}
