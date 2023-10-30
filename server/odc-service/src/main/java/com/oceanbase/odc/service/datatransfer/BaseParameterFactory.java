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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.sql.DataSource;

import org.apache.commons.collections4.CollectionUtils;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.common.util.VersionUtils;
import com.oceanbase.odc.core.session.ConnectionSessionUtil;
import com.oceanbase.odc.core.shared.constant.ConnectionAccountType;
import com.oceanbase.odc.core.shared.constant.DialectType;
import com.oceanbase.odc.core.sql.util.OBUtils;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.CsvConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferConfig;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferFormat;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferObject;
import com.oceanbase.odc.plugin.task.api.datatransfer.model.DataTransferType;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.connection.model.OBTenantEndpoint;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.tools.loaddump.common.enums.ObjectType;
import com.oceanbase.tools.loaddump.common.model.BaseParameter;
import com.oceanbase.tools.loaddump.parser.record.csv.CsvFormat;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BaseParameterFactory}
 *
 * @author yh263208
 * @date 2022-06-29 15:55
 * @since ODC_release_3.4.0
 */
@Slf4j
public abstract class BaseParameterFactory<T extends BaseParameter> {
    private final File workingDir;
    private final File logDir;
    private final ConnectionConfig target;

    public BaseParameterFactory(@NonNull File workingDir, @NonNull File logDir,
            @NonNull ConnectionConfig connectionConfig)
            throws FileNotFoundException {
        if (!workingDir.exists()) {
            throw new FileNotFoundException("Working dir does not exist, " + workingDir);
        }
        if (!workingDir.isDirectory()) {
            throw new IllegalArgumentException("Working dir is not a dir");
        }
        if (!logDir.exists()) {
            throw new FileNotFoundException("Log dir does not exists, " + logDir);
        }
        if (!logDir.isDirectory()) {
            throw new IllegalArgumentException("Log dir is not a dir");
        }
        this.workingDir = workingDir;
        this.logDir = logDir;
        this.target = connectionConfig;
    }

    public T generate(@NonNull DataTransferConfig transferConfig) throws IOException {
        T parameter = doGenerate(workingDir, target, transferConfig);
        parameter.setLogPath(logDir.toString());
        setSessionInfo(parameter, transferConfig.getSchemaName());
        setFileConfig(parameter, transferConfig, workingDir);
        parameter.setThreads(3);
        if (transferConfig.getDataTransferFormat() != DataTransferFormat.CSV) {
            return parameter;
        }
        if (transferConfig.getTransferType() == DataTransferType.EXPORT) {
            setCsvInfo(parameter, transferConfig);
        } else if (!transferConfig.isCompressed()) {
            /**
             * csv 文件导入时需要手动设置 csv 文件相关解析参数
             */
            setCsvInfo(parameter, transferConfig);
        }
        return parameter;
    }

    protected abstract T doGenerate(File workingDir, ConnectionConfig target,
            DataTransferConfig transferConfig) throws IOException;

    private void setSessionInfo(@NonNull T parameter, @NonNull String schema) {
        parameter.setHost(target.getHost());
        parameter.setPort(target.getPort());
        parameter.setPassword(target.getPassword());
        parameter.setCluster(target.getClusterName());
        parameter.setTenant(target.getTenantName());
        String username = ConnectionSessionUtil.getUserOrSchemaString(target.getUsername(), target.getDialectType());
        if (DialectType.OB_ORACLE == target.getDialectType()) {
            parameter.setUser("\"" + username + "\"");
            parameter.setDatabaseName("\"" + schema + "\"");
            parameter.setConnectDatabaseName("\"" + schema + "\"");
        } else {
            parameter.setUser(username);
            parameter.setDatabaseName("`" + schema + "`");
            parameter.setConnectDatabaseName(schema);
        }
        String version;
        DataSource ds = null;
        try {
            ds = new OBConsoleDataSourceFactory(target,
                    ConnectionAccountType.MAIN, null, false).getDataSource();
            version = OBUtils.getObVersion(ds.getConnection());
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        } finally {
            if (ds instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) ds).close();
                } catch (Exception e) {
                    // eat exception
                }
            }
        }
        if (VersionUtils.isGreaterThanOrEqualsTo(version, "4.0")) {
            parameter.setNoSys(false);
        } else {
            if (StringUtils.isNotBlank(target.getSysTenantUsername())) {
                parameter.setSysUser(target.getSysTenantUsername());
                parameter.setSysPassword(target.getSysTenantPassword());
                log.info("Sys user exists");
            } else {
                if (target.getType().isCloud()) {
                    log.info("Sys user does not exist, use cloud mode");
                    parameter.setPubCloud(true);
                } else {
                    log.info("Sys user does not exist, use no sys mode");
                    parameter.setNoSys(true);
                }
            }
        }

        OBTenantEndpoint endpoint = target.getEndpoint();
        if (Objects.nonNull(endpoint)) {
            String proxyHost = endpoint.getProxyHost();
            Integer proxyPort = endpoint.getProxyPort();
            if (StringUtils.isNotBlank(proxyHost) && Objects.nonNull(proxyPort)) {
                parameter.setSocksProxyHost(proxyHost);
                parameter.setSocksProxyPort(proxyPort.toString());
            }
        }
        if (StringUtils.isNotBlank(target.getOBTenantName())) {
            parameter.setTenant(target.getOBTenantName());
        }
    }

    private void setCsvInfo(BaseParameter parameter, DataTransferConfig transferConfig) {
        CsvConfig csvConfig = transferConfig.getCsvConfig();
        parameter.setIgnoreEmptyLine(true);
        if (csvConfig == null) {
            return;
        }
        parameter.setColumnSeparator(CsvFormat.DEFAULT.toChar(csvConfig.getColumnSeparator()));
        String lineSeparator = csvConfig.getLineSeparator();
        String realLineSeparator = "";
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
                    realLineSeparator += '\n';
                } else if (item == 'r') {
                    realLineSeparator += '\r';
                }
                transferFlag = false;
            } else {
                realLineSeparator += item;
            }
        }
        parameter.setLineSeparator(realLineSeparator);
        parameter.setSkipHeader(csvConfig.isSkipHeader());
        parameter.setColumnDelimiter(CsvFormat.DEFAULT.toChar(csvConfig.getColumnDelimiter()));
        /**
         * oracle 模式下空字符即为 null ，因此 emptyString 参数仅对 mysql 模式生效，组件默认为 \E
         */
        parameter.setEmptyString("");
        if (!csvConfig.isBlankToNull()) {
            return;
        }
        parameter.setNullString("null");
    }

    private void setFileConfig(T parameter, DataTransferConfig config, File workingDir) {
        parameter.setFilePath(workingDir.getAbsolutePath());
        parameter.setFileEncoding(config.getEncoding().getAlias());
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
            if (DialectType.OB_ORACLE == target.getDialectType()) {
                nameSet.add(StringUtils.quoteOracleIdentifier(objectName));
            } else {
                nameSet.add(StringUtils.quoteMysqlIdentifier(objectName));
            }
        }
        return whiteListMap;
    }

}
