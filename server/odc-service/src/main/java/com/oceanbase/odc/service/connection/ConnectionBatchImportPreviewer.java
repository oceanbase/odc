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
package com.oceanbase.odc.service.connection;

import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_CLUSTERNAME;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_ENVIRONMENT;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_ENVIRONMENT_DEFAULT;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_ENVIRONMENT_DEV;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_ENVIRONMENT_PROD;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_ENVIRONMENT_SIT;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_HOST;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_NAME;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_PASSWORD;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_PORT;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_SYSTENANTPASSWORD;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_SYSTENANTUSERNAME;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_TENANTNAME;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_TYPE;
import static com.oceanbase.odc.core.shared.constant.FieldName.DATASOURCE_USERNAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.authority.util.Authenticated;
import com.oceanbase.odc.core.authority.util.PreAuthenticate;
import com.oceanbase.odc.core.shared.constant.ConnectType;
import com.oceanbase.odc.core.shared.constant.ErrorCodes;
import com.oceanbase.odc.service.collaboration.environment.EnvironmentService;
import com.oceanbase.odc.service.collaboration.environment.model.Environment;
import com.oceanbase.odc.service.collaboration.environment.model.QueryEnvironmentParam;
import com.oceanbase.odc.service.common.util.FileConvertUtils;
import com.oceanbase.odc.service.connection.model.BatchImportConnection;
import com.oceanbase.odc.service.connection.model.ConnectionPreviewBatchImportResp;
import com.oceanbase.odc.service.iam.auth.AuthenticationFacade;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Authenticated
public class ConnectionBatchImportPreviewer {

    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private AuthenticationFacade authenticationFacade;
    public static final int NAME_MAX_LENGTH = 128;
    private static final Pattern SPACE_PATTERN = Pattern.compile("^(?=\\S).+(?<=[^.\\s])$");

    @PreAuthenticate(actions = "create", resourceType = "ODC_CONNECTION", isForAll = true)
    public ConnectionPreviewBatchImportResp preview(MultipartFile file) throws IOException {
        if (!checkFileType(file)) {
            return ConnectionPreviewBatchImportResp.ofFail(ErrorCodes.ImportInvalidFileType);
        }
        List<Map<String, String>> list;
        List<BatchImportConnection> batchImportConnectionList = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            list = FileConvertUtils.convertXlsRowsToMapList(inputStream);
        }
        Map<String, String> envMap = initEnvMap();
        for (Map<String, String> map : list) {
            BatchImportConnection batchImportConnection = createBatchImportConnection(map, envMap);
            batchImportConnectionList.add(batchImportConnection);
        }
        return ConnectionPreviewBatchImportResp.ofConnectionExcel(batchImportConnectionList);
    }

    private boolean checkFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (file.isEmpty()) {
            log.warn("The import file cannot be empty，fileName={}", originalFilename);
            return false;
        }

        // Excel文件格式校验
        if (!StringUtils.endsWithIgnoreCase(originalFilename, ".xls")
                && !StringUtils.endsWithIgnoreCase(originalFilename, ".xlsx")) {
            log.warn("The uploaded file is not an Excel file，fileName={}", originalFilename);
            return false;
        }
        return true;
    }

    private BatchImportConnection createBatchImportConnection(Map<String, String> map, Map<String, String> envMap) {
        BatchImportConnection batchImportConnection = new BatchImportConnection();
        Long organizationId = authenticationFacade.currentOrganizationId();
        String name = map.get(DATASOURCE_NAME.getLocalizedMessage());
        if (StringUtils.isEmpty(name) || name.length() > NAME_MAX_LENGTH || !SPACE_PATTERN.matcher(name).matches()) {
            batchImportConnection.setErrorMessage("file content error:" + DATASOURCE_NAME.getLocalizedMessage());
        } else {
            batchImportConnection.setName(name);
        }
        String type = map.get(DATASOURCE_TYPE.getLocalizedMessage());
        batchImportConnection.setType(ConnectType.valueOf(type));
        String host = map.get(DATASOURCE_HOST.getLocalizedMessage());
        if (StringUtils.isEmpty(host)) {
            batchImportConnection.setErrorMessage("file content error:" + DATASOURCE_HOST.getLocalizedMessage());
        } else {
            batchImportConnection.setHost(host);
        }
        String port = map.get(DATASOURCE_PORT.getLocalizedMessage());
        if (!StringUtils.checkPort(port)) {
            batchImportConnection.setErrorMessage("file content error:" + DATASOURCE_PORT.getLocalizedMessage());
        } else {
            batchImportConnection.setPort(Integer.parseInt(port));
        }
        String clusterName = map.get(DATASOURCE_CLUSTERNAME.getLocalizedMessage());
        batchImportConnection.setClusterName(clusterName);
        String tenantName = map.get(DATASOURCE_TENANTNAME.getLocalizedMessage());
        if (StringUtils.isEmpty(tenantName) || !SPACE_PATTERN.matcher(tenantName).matches()) {
            batchImportConnection.setErrorMessage("file content error:" + DATASOURCE_TENANTNAME.getLocalizedMessage());
        } else {
            batchImportConnection.setTenantName(tenantName);
        }
        String username = map.get(DATASOURCE_USERNAME.getLocalizedMessage());
        if (StringUtils.isEmpty(username) || !SPACE_PATTERN.matcher(username).matches()) {
            batchImportConnection.setErrorMessage("file content error:" + DATASOURCE_USERNAME.getLocalizedMessage());
        } else {
            batchImportConnection.setUsername(username);
        }
        String password = map.get(DATASOURCE_PASSWORD.getLocalizedMessage());
        batchImportConnection.setPassword(password);
        String environment = map.get(DATASOURCE_ENVIRONMENT.getLocalizedMessage());
        String envString = envMap.get(environment);
        List<Environment> environments =
                environmentService.list(organizationId, QueryEnvironmentParam.builder().build());
        Long environmentId = -1L;
        for (Environment environment1 : environments) {
            if (environment1.getName().equals(envString)) {
                environmentId = environment1.getId();
                break;
            }
        }
        batchImportConnection.setEnvironmentId(environmentId);
        String sysTenantUsername = map.get(DATASOURCE_SYSTENANTUSERNAME.getLocalizedMessage());
        batchImportConnection.setSysTenantUsername(sysTenantUsername);
        String sysTenantPassword = map.get(DATASOURCE_SYSTENANTPASSWORD.getLocalizedMessage());
        batchImportConnection.setSysTenantPassword(sysTenantPassword);
        return batchImportConnection;
    }

    private Map<String, String> initEnvMap() {
        Map<String, String> envMap = new HashMap<>();
        envMap.put(DATASOURCE_ENVIRONMENT_DEFAULT.getLocalizedMessage(),
                "${com.oceanbase.odc.builtin-resource.collaboration.environment.default.name}");
        envMap.put(DATASOURCE_ENVIRONMENT_DEV.getLocalizedMessage(),
                "${com.oceanbase.odc.builtin-resource.collaboration.environment.dev.name}");
        envMap.put(DATASOURCE_ENVIRONMENT_PROD.getLocalizedMessage(),
                "${com.oceanbase.odc.builtin-resource.collaboration.environment.prod.name}");
        envMap.put(DATASOURCE_ENVIRONMENT_SIT.getLocalizedMessage(),
                "${com.oceanbase.odc.builtin-resource.collaboration.environment.sit.name}");
        return envMap;
    }
}
