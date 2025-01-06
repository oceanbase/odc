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

package com.oceanbase.odc.plugin.task.obmysql.datatransfer.factory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.datasource.SingleConnectionDataSource;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.ConnectionInfo;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.util.ConnectionUtil;
import com.oceanbase.odc.plugin.task.obmysql.datatransfer.util.PluginUtil;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.BaseParameter;
import com.oceanbase.tools.loaddump.common.model.SessionConfig;
import com.oceanbase.tools.loaddump.common.model.storage.StorageConfig;
import com.oceanbase.tools.loaddump.compress.CompressorFactory;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseParameterFactory<T extends BaseParameter> {
    private static final String SESSION_CONFIG_FILE_PATH = "/session.config.json";

    protected final DataTransferConfig transferConfig;
    protected final File workingDir;
    protected final File logDir;

    public BaseParameterFactory(@NonNull DataTransferConfig transferConfig, File workingDir, File logDir) {
        this.transferConfig = transferConfig;
        this.workingDir = workingDir;
        this.logDir = logDir;
    }

    public T generate() throws IOException {
        T parameter = doGenerate(workingDir);
        parameter.setLogPath(logDir.getPath());
        setSessionInfo(parameter);
        setInitSqls(parameter);
        parameter.setThreads(3);
        if (transferConfig.getDataTransferFormat() != DataTransferFormat.SQL) {
            setCsvInfo(parameter);
        }
        return parameter;
    }

    protected abstract T doGenerate(File workingDir) throws IOException;

    private void setSessionInfo(@NonNull T parameter) {
        ConnectionInfo target = transferConfig.getConnectionInfo();
        parameter.setHost(target.getHost());
        parameter.setPort(target.getPort() + "");
        parameter.setPassword(target.getPassword());
        parameter.setCluster(target.getClusterName());
        parameter.setTenant(target.getTenantName());
        String username = ConnectionSessionUtil.getUserOrSchemaString(target.getUsername(),
                target.getConnectType().getDialectType());
        if (DialectType.OB_ORACLE == target.getConnectType().getDialectType()) {
            parameter.setUser("\"" + username + "\"");
            parameter.setDatabaseName("\"" + transferConfig.getSchemaName() + "\"");
            parameter.setConnectDatabaseName("\"" + transferConfig.getSchemaName() + "\"");
        } else {
            parameter.setUser(username);
            parameter.setDatabaseName(transferConfig.getSchemaName());
            parameter.setConnectDatabaseName(transferConfig.getSchemaName());
        }

        try (SingleConnectionDataSource dataSource =
                ConnectionUtil.getDataSource(transferConfig.getConnectionInfo(), transferConfig.getSchemaName());
                Connection conn = dataSource.getConnection()) {
            String version = PluginUtil.getInformationExtension(transferConfig.getConnectionInfo()).getDBVersion(conn);
            if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
                parameter.setNoSys(false);
            } else {
                if (StringUtils.isNotBlank(target.getSysTenantUsername())) {
                    parameter.setSysUser(target.getSysTenantUsername());
                    parameter.setSysPassword(target.getSysTenantPassword());
                    log.info("Sys user exists");
                } else {
                    if (target.getConnectType().isCloud()) {
                        log.info("Sys user does not exist, use cloud mode");
                        parameter.setPubCloud(true);
                    } else {
                        log.info("Sys user does not exist, use no sys mode");
                        parameter.setNoSys(true);
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("Failed to get version info, transfer will continue without sys tenant, reason:{}",
                    e.getMessage());
            parameter.setNoSys(true);
        }

        if (StringUtils.isNotBlank(target.getProxyHost()) && Objects.nonNull(target.getProxyPort())) {
            parameter.setSocksProxyHost(target.getProxyHost());
            parameter.setSocksProxyPort(target.getProxyPort().toString());
        }
        if (StringUtils.isNotBlank(target.getOBTenant())) {
            parameter.setTenant(target.getOBTenant());
        }
    }

    private void setInitSqls(BaseParameter parameter) throws IOException {
        File sessionFile = new File(workingDir, "session.config");
        IOUtils.copy(getClass().getResource(SESSION_CONFIG_FILE_PATH), sessionFile);
        SessionConfig sessionConfig = SessionConfig.fromJson(sessionFile);

        sessionConfig.setJdbcOption("useServerPrepStmts", transferConfig.isUsePrepStmts() + "");
        sessionConfig.setJdbcOption("useCursorFetch", transferConfig.isUsePrepStmts() + "");

        sessionConfig.setJdbcOption("sendConnectionAttributes", "true");
        sessionConfig.setJdbcOption("defaultConnectionAttributesBanList", "__client_ip");

        if (StringUtils.isNotBlank(transferConfig.getConnectionInfo().getProxyHost())
                && Objects.nonNull(transferConfig.getConnectionInfo().getProxyPort())) {
            sessionConfig.setJdbcOption("socksProxyHost", transferConfig.getConnectionInfo().getProxyHost());
            sessionConfig.setJdbcOption("socksProxyPort", transferConfig.getConnectionInfo().getProxyPort() + "");
        }

        Optional.ofNullable(transferConfig.getExecutionTimeoutSeconds())
                .ifPresent(timeout -> {
                    sessionConfig.setJdbcOption("socketTimeout", timeout * 1000 + "");
                    sessionConfig.setJdbcOption("connectTimeout", timeout * 1000 + "");
                    sessionConfig.addInitSql4Both("set ob_query_timeout = " + timeout * 1000000L);
                });

        ConnectionInfo connectionInfo = transferConfig.getConnectionInfo();
        if (MapUtils.isNotEmpty(connectionInfo.getJdbcUrlParameters())) {
            connectionInfo.getJdbcUrlParameters().forEach(
                    (key, value) -> sessionConfig.setJdbcOption(key, value.toString()));
        }
        if (CollectionUtils.isNotEmpty(connectionInfo.getSessionInitScripts())) {
            connectionInfo.getSessionInitScripts().forEach(sessionConfig::addInitSql4Both);
        }

        parameter.setSessionConfig(sessionConfig);
        FileUtils.deleteQuietly(sessionFile);
    }

    private void setCsvInfo(BaseParameter parameter) {
        CsvConfig csvConfig = transferConfig.getCsvConfig();
        parameter.setIgnoreEmptyLine(true);
        if (csvConfig == null) {
            return;
        }
        parameter.setColumnSeparator(csvConfig.getColumnSeparator());
        String lineSeparator = csvConfig.getLineSeparator();
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
        parameter.setLineSeparator(realLineSeparator.toString());
        parameter.setSkipHeader(csvConfig.isSkipHeader());
        parameter.setColumnDelimiter(csvConfig.getColumnDelimiter());
        /*
         * oracle 模式下空字符即为 null ，因此 emptyString 参数仅对 mysql 模式生效，组件默认为 \E
         */
        parameter.setEmptyString("");
        if (!csvConfig.isBlankToNull()) {
            return;
        }
        parameter.setNullString("null");
    }

    void setFileConfig(T parameter, File workingDir) {
        String filePath = workingDir.getAbsolutePath();
        parameter.setFilePath(filePath);
        StorageConfig storageConfig = StorageConfig.create(workingDir.toURI().getRawPath());
        storageConfig.withCompress(
                CompressorFactory.getCompressor(parameter.getCompressAlgo(), parameter.getCompressLevel()));
        parameter.setStorageConfig(storageConfig);
        parameter.setTmpPath(filePath);
        parameter.setFileEncoding(transferConfig.getEncoding().getAlias());
    }

    protected Map<ObjectType, Set<String>> getWhiteListMap(List<DataTransferObject> objectList,
            Predicate<DataTransferObject> predicate) {
        Map<ObjectType, Set<String>> whiteListMap = new HashMap<>();
        if (CollectionUtils.isEmpty(objectList)) {
            return whiteListMap;
        }
        for (DataTransferObject dbObject : objectList) {
            if (!predicate.test(dbObject)) {
                log.info("Invalid db object type found, object={}", dbObject);
                continue;
            }
            String objectName = StringUtils.unquoteOracleIdentifier(dbObject.getObjectName());
            if (StringUtils.isBlank(objectName)) {
                throw new IllegalArgumentException("Can not accept a blank object name");
            }
            Set<String> nameSet = whiteListMap.computeIfAbsent(dbObject.getDbObjectType(), k -> new HashSet<>());
            if (StringUtils.isNotEmpty(transferConfig.getQuerySql())) {
                // do not quote table name for result-set-export task, because ob-loader-dumper will quote it in
                // insertion repeatedly
                nameSet.add(objectName);
                continue;
            }
            if (transferConfig.getConnectionInfo().getConnectType().getDialectType().isOracle()) {
                nameSet.add(StringUtils.quoteOracleIdentifier(objectName));
            } else {
                nameSet.add(StringUtils.quoteMysqlIdentifier(objectName));
            }
        }
        return whiteListMap;
    }
}
