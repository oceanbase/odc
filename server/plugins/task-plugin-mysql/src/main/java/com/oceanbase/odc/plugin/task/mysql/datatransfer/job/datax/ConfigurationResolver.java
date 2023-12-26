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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.datamasking.masker.AbstractDataMasker;
import com.oceanbase.odc.core.shared.exception.UnsupportedException;
import com.oceanbase.odc.core.shared.model.TableIdentity;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvColumnMapping;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ObjectResult;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.common.Constants;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobConfiguration;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobContent;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.JobContent.Parameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.GroovyTransformerParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.MySQLReaderPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.MySQLWriterPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.MySQLWriterPluginParameter.DataXConnection;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.TxtPluginParameter.DataXCsvConfig;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.TxtReaderPluginParameter;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.TxtReaderPluginParameter.Column;
import com.oceanbase.odc.plugin.task.mysql.datatransfer.job.datax.model.parameter.TxtWriterPluginParameter;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

public class ConfigurationResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger("DataTransferLogger");

    @SuppressWarnings("all")
    /**
     * <pre>
     * 
     * When importing data, user can configure column mapping relationships. OB-LOADER-DUMPER provides a
     * direct interface to enter the mapping. In datax, the way we configure the mapping relationship
     * is: 
     * 1. Specify the index and type of the columns to be read in the {@link TxtReaderPluginParameter#column}. 
     * 2. Specify the column names to be written in the {@link MySQLWriterPluginParameter#column}. The column 
     * names must be in strict order.
     *
     * </pre>
     */
    public static JobConfiguration buildJobConfigurationForImport(DataTransferConfig baseConfig, String jdbcUrl,
            ObjectResult object, URL resource, List<DBTableColumn> columns) throws URISyntaxException {
        if (baseConfig.getDataTransferFormat() == DataTransferFormat.SQL) {
            throw new UnsupportedException("SQL files should not be imported by DataX!");
        }

        JobConfiguration jobConfig = new JobConfiguration();
        JobContent jobContent = new JobContent();

        Long errorRecordLimit = baseConfig.isStopWhenError() ? 0L : null;
        jobConfig.getSetting().getErrorLimit().setRecord(errorRecordLimit);

        List<CsvColumnMapping> columnMappings = getColumnMapping(baseConfig, columns, object.getName());
        jobContent.setReader(createTxtReaderParameter(baseConfig, resource, columnMappings));
        jobContent.setWriter(createMySQLWriterParameter(baseConfig, jdbcUrl, object.getName(), columnMappings));
        jobConfig.setContent(new JobContent[] {jobContent});
        return jobConfig;
    }

    public static JobConfiguration buildJobConfigurationForExport(File workingDir, DataTransferConfig baseConfig,
            String jdbcUrl, String table, List<DBTableColumn> columns) {
        JobConfiguration jobConfig = new JobConfiguration();
        JobContent jobContent = new JobContent();

        Long errorRecordLimit = baseConfig.isStopWhenError() ? 0L : null;
        jobConfig.getSetting().getErrorLimit().setRecord(errorRecordLimit);

        jobContent.setReader(createMySQLReaderParameter(baseConfig, jdbcUrl, table));
        jobContent.setWriter(createTxtWriterParameter(workingDir, baseConfig, table,
                columns.stream().map(DBTableColumn::getName).collect(Collectors.toList())));
        Map<TableIdentity, Map<String, AbstractDataMasker>> maskConfigs = baseConfig.getMaskConfig();
        if (MapUtils.isNotEmpty(maskConfigs)) {
            jobContent.setTransformer(createTransformerParameters(maskConfigs, columns));
        }
        jobConfig.setContent(new JobContent[] {jobContent});
        return jobConfig;
    }

    private static Parameter createTxtReaderParameter(DataTransferConfig baseConfig, URL input,
            List<CsvColumnMapping> columnMappings) throws URISyntaxException {

        Parameter reader = new Parameter();
        TxtReaderPluginParameter pluginParameter = new TxtReaderPluginParameter();
        reader.setName(Constants.TXT_FILE_READER);
        reader.setParameter(pluginParameter);

        // common
        pluginParameter.setEncoding(baseConfig.getEncoding().getAlias());
        pluginParameter.setFileFormat("csv");
        // path
        pluginParameter.setPath(Collections.singletonList(input.toURI().getPath()));
        // column
        pluginParameter.setColumn(columnMappings.stream()
                .map(mapping -> new Column(mapping.getSrcColumnPosition(), "string"))
                .collect(Collectors.toList()));
        // csv config
        if (Objects.nonNull(baseConfig.getCsvConfig())) {
            pluginParameter.setCsvReaderConfig(getDataXCsvConfig(baseConfig));
            pluginParameter.setSkipHeader(baseConfig.getCsvConfig().isSkipHeader());
            pluginParameter.setNullFormat(baseConfig.getCsvConfig().isBlankToNull() ? "null" : "");
        }

        return reader;
    }

    private static Parameter createTxtWriterParameter(File workingDir, DataTransferConfig baseConfig, String table,
            List<String> columns) {
        Parameter writer = new Parameter();
        TxtWriterPluginParameter pluginParameter = new TxtWriterPluginParameter();
        writer.setName(Constants.TXT_FILE_WRITER);
        writer.setParameter(pluginParameter);

        // common
        pluginParameter.setEncoding(baseConfig.getEncoding().getAlias());
        pluginParameter.setFileFormat(baseConfig.getDataTransferFormat() == DataTransferFormat.SQL ? "sql" : "csv");
        // path
        pluginParameter.setPath(Paths.get(workingDir.getPath(), "data", "TABLE").toString());
        pluginParameter.setFileName(table + baseConfig.getDataTransferFormat().getExtension());
        // header
        pluginParameter.setHeader(columns);
        pluginParameter.setTable(table);
        // sql config
        pluginParameter.setQuoteChar("`");
        if (Objects.nonNull(baseConfig.getBatchCommitNum())) {
            pluginParameter.setCommitSize(baseConfig.getBatchCommitNum());
        }
        // csv config
        if (Objects.nonNull(baseConfig.getCsvConfig())) {
            pluginParameter.setLineDelimiter(getRealLineSeparator(baseConfig.getCsvConfig().getLineSeparator()));
            pluginParameter.setCsvWriterConfig(getDataXCsvConfig(baseConfig));
            pluginParameter.setSkipHeader(baseConfig.getCsvConfig().isSkipHeader());
            pluginParameter.setNullFormat(baseConfig.getCsvConfig().isBlankToNull() ? "null" : "");
            if (baseConfig.getCsvConfig().isSkipHeader()
                    && baseConfig.getDataTransferFormat() != DataTransferFormat.SQL) {
                pluginParameter.setHeader(null);
            }
        }

        return writer;
    }

    private static Parameter createMySQLReaderParameter(DataTransferConfig baseConfig, String url, String table) {
        Parameter reader = new Parameter();
        MySQLReaderPluginParameter pluginParameter = new MySQLReaderPluginParameter();
        reader.setName(Constants.MYSQL_READER);
        reader.setParameter(pluginParameter);

        // connection
        pluginParameter.setUsername(baseConfig.getConnectionInfo().getUserNameForConnect());
        pluginParameter.setPassword(baseConfig.getConnectionInfo().getPassword());
        MySQLReaderPluginParameter.DataXConnection connection = new MySQLReaderPluginParameter.DataXConnection(
                new String[] {url});
        // querySql
        if (Objects.nonNull(baseConfig.getQuerySql())) {
            connection.setQuerySql(new String[] {baseConfig.getQuerySql()});
        } else {
            connection.setTable(new String[] {table});
        }
        pluginParameter.setConnection(Collections.singletonList(connection));

        return reader;
    }

    private static Parameter createMySQLWriterParameter(DataTransferConfig baseConfig, String url, String table,
            List<CsvColumnMapping> columnMappings) {
        Parameter writer = new Parameter();
        MySQLWriterPluginParameter pluginParameter = new MySQLWriterPluginParameter();
        writer.setName(Constants.MYSQL_WRITER);
        writer.setParameter(pluginParameter);

        pluginParameter.setWriteMode("insert");
        // connection
        pluginParameter.setUsername(baseConfig.getConnectionInfo().getUserNameForConnect());
        pluginParameter.setPassword(baseConfig.getConnectionInfo().getPassword());
        DataXConnection connection = new DataXConnection(url, new String[] {table});
        pluginParameter.setConnection(Collections.singletonList(connection));
        // preSql
        List<String> preSql = Lists.newArrayList(Constants.DISABLE_FK);
        if (baseConfig.isTruncateTableBeforeImport()) {
            preSql.add("TRUNCATE TABLE " + table);
        }
        pluginParameter.setPreSql(preSql);
        // postSql
        pluginParameter.setPostSql(Collections.singletonList(Constants.ENABLE_FK));
        // column
        pluginParameter.setColumn(columnMappings.stream()
                .map(CsvColumnMapping::getDestColumnName)
                .collect(Collectors.toList()));

        return writer;
    }

    private static List<Parameter> createTransformerParameters(
            Map<TableIdentity, Map<String, AbstractDataMasker>> maskConfigs, List<DBTableColumn> columns) {
        Parameter transformer = new Parameter();
        GroovyTransformerParameter pluginParameter = new GroovyTransformerParameter();
        transformer.setName(Constants.GROOVY_TRANSFORMER);
        transformer.setParameter(pluginParameter);

        pluginParameter.setCode(GroovyMaskRuleGenerator.generate(maskConfigs, columns));

        return Collections.singletonList(transformer);
    }

    private static DataXCsvConfig getDataXCsvConfig(DataTransferConfig baseConfig) {
        CsvConfig csvConfig = baseConfig.getCsvConfig();
        if (csvConfig == null) {
            csvConfig = new CsvConfig();
        }
        return DataXCsvConfig.builder()
                .textQualifier(csvConfig.getColumnDelimiter())
                .delimiter(csvConfig.getColumnSeparator())
                .recordDelimiter(getRealLineSeparator(csvConfig.getLineSeparator()))
                .build();
    }

    private static String getRealLineSeparator(String lineSeparator) {
        StringBuilder realLineSeparator = new StringBuilder();
        int length = lineSeparator.length();
        boolean transferFlag = false;
        for (int i = 0; i < length; i++) {
            char item = lineSeparator.charAt(i);
            if (item == '\\') {
                transferFlag = true;
                continue;
            }
            if (transferFlag) {
                if (item == 'n') {
                    realLineSeparator.append('\n');
                } else if (item == 'r') {
                    realLineSeparator.append('\r');
                }
                transferFlag = false;
            } else {
                realLineSeparator.append(item);
            }
        }
        return realLineSeparator.toString();
    }

    private static List<CsvColumnMapping> getColumnMapping(DataTransferConfig baseConfig,
            List<DBTableColumn> tableColumns, String table) {
        List<CsvColumnMapping> mappings = baseConfig.getCsvColumnMappings();
        if (CollectionUtils.isEmpty(mappings)) {
            // if mappings are empty, the number of columns in source file and target table must be identical
            mappings = new ArrayList<>();
            for (int i = 0; i < tableColumns.size(); i++) {
                CsvColumnMapping mapping = new CsvColumnMapping();
                mapping.setSrcColumnPosition(i);
                mapping.setDestColumnPosition(i);
                mapping.setDestColumnName(tableColumns.get(i).getName());
                mapping.setDestColumnType(tableColumns.get(i).getTypeName());
                mappings.add(mapping);
            }
        }
        return mappings.stream()
                .filter(mapping -> !isNeedToSkip(baseConfig.getSkippedDataType(), mapping, table))
                .sorted(Comparator.comparingInt(CsvColumnMapping::getSrcColumnPosition))
                .collect(Collectors.toList());
    }

    private static boolean isNeedToSkip(List<String> skippedDataTypes, CsvColumnMapping column, String table) {
        if (CollectionUtils.isEmpty(skippedDataTypes)) {
            return false;
        }
        if (Objects.isNull(column.getDestColumnType())) {
            return true;
        }
        boolean matched = skippedDataTypes.stream()
                .anyMatch(type -> StringUtils.equalsIgnoreCase(type, column.getDestColumnType()));
        if (matched) {
            LOGGER.info("The type of column [{}].[{}] is {}, needs to be skipped.",
                    table, column.getDestColumnName(), column.getDestColumnType());
        }
        return matched;
    }

}
