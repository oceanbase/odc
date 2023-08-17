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
package com.oceanbase.odc.core.sql.execute.cache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import com.oceanbase.odc.core.sql.execute.cache.model.BinaryContentMetaData;
import com.oceanbase.odc.core.sql.execute.cache.table.ResultSetVirtualTable;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualElement;
import com.oceanbase.odc.core.sql.execute.cache.table.VirtualTable;
import com.oceanbase.odc.test.database.TestDBConfigurations;
import com.oceanbase.tools.dbbrowser.model.datatype.DataTypeUtil;

/**
 * Test case for {@code ResultSetCachedElementFactory} and {@code ResultSetVirtualTable}
 *
 * @author yh263208
 * @date 2021-11-05 18:42
 * @since ODC_release_3.2.2
 */
public class ResultSetBasedElementTest {
    private static final String CACHE_DATA_DIR_NAME = "ResultSetElementFactoryTest/cache".toLowerCase();
    private static final String BINARY_DATA_DIR_NAME = "ResultSetElementFactoryTest/data".toLowerCase();
    private final Random random = new Random();
    private final String tableName = "test_table_" + random.nextInt(100000);
    private final Long generateLineNum = 20L;
    private final DataSource dataSource =
            TestDBConfigurations.getInstance().getTestOBMysqlConfiguration().getDataSource();

    @Before
    public void setUp() throws Exception {
        File dataDir = new File(getBinaryFilePath(CACHE_DATA_DIR_NAME));
        for (File file : dataDir.listFiles()) {
            FileUtils.forceDelete(file);
        }
        dataDir = new File(getBinaryFilePath(BINARY_DATA_DIR_NAME));
        for (File file : dataDir.listFiles()) {
            FileUtils.forceDelete(file);
        }
        initJdbc();
    }

    @Test
    public void testElementFactory() throws Exception {
        BinaryDataManager dataManager = getDataManager();
        String tableId = UUID.randomUUID().toString().replace("-", "");
        consumeResultSet(resultSet -> {
            try {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                long line = 0;
                while (resultSet.next()) {
                    ResultSetCachedElementFactory factory =
                            new ResultSetCachedElementFactory(resultSet, dataManager);
                    for (int i = 0; i < columnCount; i++) {
                        VirtualElement element = factory.generateElement(tableId, line++, i);
                        String dataType = metaData.getColumnTypeName(i + 1);
                        Assert.assertEquals(dataType, element.dataTypeName());
                        if (DataTypeUtil.isBinaryType(dataType)) {
                            List<String> lineOne =
                                    IOUtils.readLines(dataManager.read((BinaryContentMetaData) element.getContent()));
                            List<String> lineTwo = IOUtils.readLines(resultSet.getBinaryStream(i + 1));
                            Assert.assertEquals(lineOne, lineTwo);
                        } else {
                            Object content = resultSet.getObject(i + 1);
                            Assert.assertEquals(content, element.getContent());
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testVirutalTableConstruct() throws Exception {
        VirtualTable virtualTable = generateVirtualTableFromResultSet();
        Assert.assertEquals(generateLineNum, virtualTable.count());
    }

    private VirtualTable generateVirtualTableFromResultSet() throws Exception {
        BinaryDataManager dataManager = getDataManager();
        String tableId = UUID.randomUUID().toString().replace("-", "");
        ResultSetVirtualTable virtualTable =
                new ResultSetVirtualTable(tableId, new CacheColumnPredicate());
        consumeResultSet(resultSet -> {
            try {
                long line = 0;
                while (resultSet.next()) {
                    virtualTable.addLine(line++, resultSet,
                            new ResultSetCachedElementFactory(resultSet, dataManager));
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return virtualTable;
    }

    private void consumeResultSet(Consumer<ResultSet> consumer) throws Exception {
        try (Connection connection = this.dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String sql = "select * from " + tableName;
                try (ResultSet resultSet = statement.executeQuery(sql)) {
                    consumer.accept(resultSet);
                }
            }
        }
    }

    private void initJdbc() throws Exception {
        try (Connection connection = this.dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                String sql = String.format("drop table %s", tableName);
                statement.executeUpdate(sql);
            } catch (Exception ignored) {
                // ignore
            }
            try (Statement statement = connection.createStatement()) {
                String createSql =
                        String.format("create table %s (`name` varchar(64), age int, content blob)", tableName);
                statement.executeUpdate(createSql);
            }
            try (PreparedStatement preparedStatement = connection
                    .prepareStatement(String.format("insert into %s(`name`,age,content) values(?,?,?)", tableName))) {
                for (int i = 0; i < generateLineNum; i++) {
                    preparedStatement.setString(1, i + "_name");
                    preparedStatement.setInt(2, i + 22);
                    preparedStatement.setBinaryStream(3, getInputContentStream(getInputContent()));
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }
            try (PreparedStatement preparedStatement =
                    connection.prepareStatement(String.format("select count(1) from %s", tableName))) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Assert.assertTrue(resultSet.next());
                    Assert.assertEquals((Long) resultSet.getLong(1), generateLineNum);
                }
            }
        }
    }

    private String getBinaryFilePath(String middlePath) {
        File file;
        try {
            file = new File(ResourceUtils.getURL("classpath:").getPath() + "/" + middlePath);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    throw new Exception("Failed to create dir");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("fail to get dir");
        }
        return file.getAbsolutePath();
    }

    private FileBaseBinaryDataManager getDataManager() throws IOException {
        return new FileBaseBinaryDataManager(getBinaryFilePath(BINARY_DATA_DIR_NAME));
    }

    private InputStream getInputContentStream(String inputContent) {
        return new ByteArrayInputStream(inputContent.getBytes());
    }

    private String getInputContent() {
        int contentSizeBytes = random.nextInt(5 * 1024);
        char[] chars = new char[contentSizeBytes];
        for (int i = 0; i < contentSizeBytes; i++) {
            chars[i] = choseChar();
        }
        return new String(chars);
    }

    private char choseChar() {
        if (random.nextBoolean()) {
            return (char) (random.nextInt(26) + 65);
        }
        return (char) (random.nextInt(26) + 97);
    }

}
