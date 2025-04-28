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
package com.oceanbase.odc.service.onlineschemachange.oscfms.action.odc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.oceanbase.odc.common.json.JsonUtils;
import com.oceanbase.odc.common.util.StringUtils;
import com.oceanbase.odc.core.session.ConnectionSession;
import com.oceanbase.odc.service.config.SystemConfigService;
import com.oceanbase.odc.service.config.model.Configuration;
import com.oceanbase.odc.service.connection.model.ConnectionConfig;
import com.oceanbase.odc.service.db.browser.DBSchemaAccessors;
import com.oceanbase.odc.service.onlineschemachange.configuration.OnlineSchemaChangeProperties;
import com.oceanbase.odc.service.onlineschemachange.ddl.DdlUtils;
import com.oceanbase.odc.service.onlineschemachange.fsm.Action;
import com.oceanbase.odc.service.onlineschemachange.model.OnlineSchemaChangeScheduleTaskParameters;
import com.oceanbase.odc.service.onlineschemachange.oms.request.CreateOceanBaseDataSourceRequest;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionContext;
import com.oceanbase.odc.service.onlineschemachange.oscfms.OscActionResult;
import com.oceanbase.odc.service.onlineschemachange.oscfms.action.oms.OmsRequestUtil;
import com.oceanbase.odc.service.onlineschemachange.oscfms.state.OscStates;
import com.oceanbase.odc.service.resource.ResourceLocation;
import com.oceanbase.odc.service.resource.ResourceManager;
import com.oceanbase.odc.service.resource.ResourceWithID;
import com.oceanbase.odc.service.resource.k8s.K8sResourceUtil;
import com.oceanbase.odc.service.session.factory.OBConsoleDataSourceFactory;
import com.oceanbase.odc.service.task.caller.ResourceIDUtil;
import com.oceanbase.odc.service.task.config.K8sProperties;
import com.oceanbase.odc.service.task.resource.AbstractK8sResourceOperatorBuilder;
import com.oceanbase.odc.service.task.resource.Constants;
import com.oceanbase.odc.service.task.resource.K8sPodResource;
import com.oceanbase.tools.dbbrowser.model.DBTableColumn;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * @author longpeng.zlp
 * @date 2025/3/19 16:31
 */
@Slf4j
public class OdcCreateDataTaskAction implements Action<OscActionContext, OscActionResult> {
    private final SystemConfigService systemConfigService;
    private final ResourceManager resourceManager;
    private final OnlineSchemaChangeProperties oscProperties;

    public OdcCreateDataTaskAction(@NonNull SystemConfigService systemConfigService,
            @NonNull ResourceManager resourceManager,
            @NonNull OnlineSchemaChangeProperties oscProperties) {
        this.systemConfigService = systemConfigService;
        this.resourceManager = resourceManager;
        this.oscProperties = oscProperties;
    }

    public static String buildUrlPrefix(String ip, Integer port) {
        return "http://" + ip + ":" + port;
    }

    @Override
    public OscActionResult execute(OscActionContext context) throws Exception {
        OnlineSchemaChangeScheduleTaskParameters parameters = context.getTaskParameter();
        // 1. check if task worker node has been created
        if (StringUtils.isEmpty(parameters.getOdcCommandURl()) && !prepareMigrateEnv(context, parameters)) {
            log.info("ODCCreateDataTaskAction: prepare migrate env not ready yet, taskID={}",
                    context.getScheduleTask().getId());
            return new OscActionResult(OscStates.CREATE_DATA_TASK.name(), null, OscStates.CREATE_DATA_TASK.name());
        }
        // 2. check if task supervisor node has ready
        if (!OscCommandUtil.isOSCMigrateSupervisorAlive(parameters.getOdcCommandURl())) {
            log.info("ODCCreateDataTaskAction: supervisor service  not ready yet, taskID={}",
                    context.getScheduleTask().getId());
            return new OscActionResult(OscStates.CREATE_DATA_TASK.name(), null, OscStates.CREATE_DATA_TASK.name());
        }
        // 3. start task
        startTask(context, parameters);
        return new OscActionResult(OscStates.CREATE_DATA_TASK.name(), null, OscStates.MONITOR_DATA_TASK.name());
    }

    protected void startTask(OscActionContext context, OnlineSchemaChangeScheduleTaskParameters parameters)
            throws Exception {
        log.info("ODCCreateDataTaskAction: start migrate for taskID={}", context.getScheduleTask().getId());
        Map<String, String> startConfigs = new LinkedHashMap<>();
        ConnectionConfig connectionConfig = context.getConnectionProvider().connectionConfig();
        fillDataSourceRelatedConfigs(context, connectionConfig, startConfigs);
        fillOscTableRelatedConfigs(context, connectionConfig, startConfigs);
        SupervisorResponse startTaskResp = OscCommandUtil.startTask(parameters.getOdcCommandURl(), startConfigs);
        if (null == startTaskResp || !startTaskResp.isSuccess()) {
            log.info("ODCCreateDataTaskAction: start task by supervisor failed, taskID={}, response = {}",
                    context.getScheduleTask().getId(), startTaskResp);
            throw new RuntimeException("start osc migrate task failed, taskID= " + context.getScheduleTask().getId());
        }
        log.info("ODCCreateDataTaskAction: start task by supervisor done, taskID={}, response = {}",
                context.getScheduleTask().getId(), startTaskResp);
    }

    protected void fillOscTableRelatedConfigs(OscActionContext context, ConnectionConfig connectionConfig,
            Map<String, String> configs) {
        OnlineSchemaChangeScheduleTaskParameters parameters = context.getTaskParameter();
        configs.put("dbname", parameters.getDatabaseName());
        configs.put("tenantName", connectionConfig.getTenantName());
        configs.put("sourceTableName", parameters.getOriginTableName());
        configs.put("targetTableName", parameters.getNewTableName());
        // add col mapper
        List<String> columns = getValidColumns(context);
        Map<String, String> targetToSrcColumnMapper = new LinkedHashMap<>();
        for (String c : columns) {
            targetToSrcColumnMapper.put(c, c);
        }
        configs.put("targetToSrcColMapper", JsonUtils.toJson(targetToSrcColumnMapper));
        // add rate limiter
        if (parameters.getRateLimitConfig() != null && null != parameters.getRateLimitConfig().getRowLimit()) {
            configs.put("throttleRps", parameters.getRateLimitConfig().getRowLimit().toString());
        }
    }

    protected List<String> getValidColumns(OscActionContext context) {
        OnlineSchemaChangeScheduleTaskParameters taskParam = context.getTaskParameter();
        // target missed columns, use target columns
        if (CollectionUtils.isNotEmpty(taskParam.getFilterColumns())) {
            return taskParam.getFilterColumns();
        }
        ConnectionSession session = null;
        try {
            session = context.getConnectionProvider().createConnectionSession();
            // use src tables
            return DBSchemaAccessors.create(session).listTableColumns(taskParam.getDatabaseName(),
                    DdlUtils.getUnwrappedName(taskParam.getOriginTableNameUnwrapped())).stream()
                    .map(DBTableColumn::getName).collect(Collectors.toList());
        } finally {
            if (null != session) {
                session.expire();
            }
        }
    }

    /**
     * fill connection info and ob log connection info
     * 
     * @param context
     * @param connectionConfig
     * @param configs
     */
    protected void fillDataSourceRelatedConfigs(OscActionContext context, ConnectionConfig connectionConfig,
            Map<String, String> configs) {
        ConnectionSession connectionSession = null;
        try {
            connectionSession = context.getConnectionProvider().createConnectionSession();
            CreateOceanBaseDataSourceRequest dataSourceRequest = OmsRequestUtil.getCreateDataSourceRequest(
                    connectionConfig, context.getConnectionProvider().createConnectionSession(),
                    context.getTaskParameter(), oscProperties);
            configs.put("databaseUrl", dataSourceRequest.getIp() + ":" + dataSourceRequest.getPort());
            configs.put("databaseUser", OBConsoleDataSourceFactory.getUsername(connectionConfig));
            configs.put("databasePassword", dataSourceRequest.getPassword());
            configs.put("crawlerClusterURL", dataSourceRequest.getConfigUrl());
            configs.put("crawlerClusterUser", dataSourceRequest.getDrcUserName());
            configs.put("crawlerClusterPassword", dataSourceRequest.getDrcPassword());
            configs.put("crawlerClusterAppName", dataSourceRequest.getCluster());
        } finally {
            if (null != connectionSession) {
                connectionSession.expire();
            }
        }
    }

    // prepare node
    public boolean prepareMigrateEnv(OscActionContext context, OnlineSchemaChangeScheduleTaskParameters parameters)
            throws Exception {
        // try create node
        tryCreateMigrateNode(context, parameters);
        // check node has created
        return waitMigrateNodeReady(context, parameters);
    }

    protected boolean waitMigrateNodeReady(OscActionContext context,
            OnlineSchemaChangeScheduleTaskParameters parameters)
            throws Exception {
        if (StringUtils.isNotEmpty(parameters.getOdcCommandURl())) {
            return true;
        }
        Long id = parameters.getResourceID();
        K8sPodResource podResource = K8sResourceUtil.queryIpAndAddress(resourceManager, id);
        String commandURl = tryChooseUrl(podResource, parameters.getK8sMapperPort());
        if (StringUtils.isEmpty(commandURl)) {
            log.info("ODCCreateDataTaskAction: node not ready yet for resourceID={}, taskID={}", id,
                    context.getScheduleTask().getId());
            return false;
        }
        log.info("ODCCreateDataTaskAction: node ready with url = {}, resourceID={}, taskID={}", commandURl, id,
                context.getScheduleTask().getId());
        parameters.setOdcCommandURl(commandURl);
        context.getScheduleTaskRepository().updateTaskParameters(context.getScheduleTask().getId(),
                JsonUtils.toJson(parameters));
        return true;
    }

    protected void tryCreateMigrateNode(OscActionContext context, OnlineSchemaChangeScheduleTaskParameters parameters)
            throws Exception {
        // check prev create resourceID
        if (parameters.getResourceID() != null) {
            return;
        }
        long taskId = context.getScheduleTask().getId();
        K8sProperties k8sProperties = buildK8sProperties(context.getConnectionProvider().connectionConfig());
        log.info("ODCCreateDataTaskAction: create taskId = {}, with k8s properties={}",
                context.getScheduleTask().getId(), k8sProperties);
        ResourceLocation resourceLocation = new ResourceLocation(k8sProperties.getRegion(), k8sProperties.getGroup());
        boolean enablePortMapper = getK8sEnablePortMapper();
        int mappedPort = -1;
        List<Pair<Integer, Integer>> portMapper = new ArrayList<>();
        if (enablePortMapper) {
            portMapper = K8sResourceUtil.buildRandomPortMapper(18001);
            mappedPort = portMapper.get(0).getRight();
        }
        ResourceWithID<K8sPodResource> resource = K8sResourceUtil.createK8sPodResource(resourceManager,
                resourceLocation, getK8sImplType(), k8sProperties.getPodImageName(), k8sProperties, 10000000 + taskId,
                portMapper,
                18001);
        log.info("ODCCreateDataTaskAction: create k8s resource, resourceId = {}", resource);
        parameters.setResourceID(resource.getId());
        parameters.setK8sMapperPort(mappedPort);
        String commandURl = tryChooseUrl(resource.getResource(), mappedPort);
        if (null != commandURl) {
            parameters.setOdcCommandURl(commandURl);
        }
        try {
            context.getScheduleTaskRepository().updateTaskParameters(taskId, JsonUtils.toJson(parameters));
        } catch (Throwable e) {
            resourceManager.destroy(resource.getId());
            throw e;
        }
    }

    protected String tryChooseUrl(K8sPodResource resource, Integer mapperPort) {
        String podIp = resource.getPodIpAddress();
        String candidateUrl = buildUrlPrefix(podIp, 18001);
        if (StringUtils.isNotEmpty(podIp) && !StringUtils.equalsIgnoreCase(podIp, Constants.RESOURCE_NULL_HOST)
                && OscCommandUtil.isOSCMigrateSupervisorAlive(candidateUrl)) {
            log.info("ODCCreateDataTaskAction: k8s ready, podIP reached, candidateUrl = {} returned", candidateUrl);
            return candidateUrl;
        }
        String hostIp = resource.getHostIpAddress();
        candidateUrl = buildUrlPrefix(hostIp, mapperPort);
        if (StringUtils.isNotEmpty(hostIp) && !StringUtils.equalsIgnoreCase(hostIp, Constants.RESOURCE_NULL_HOST)
                && null != mapperPort && mapperPort > 0 && OscCommandUtil.isOSCMigrateSupervisorAlive(candidateUrl)) {
            log.info("ODCCreateDataTaskAction: k8s ready, hostIP reached, candidateUrl = {} returned", candidateUrl);
            return candidateUrl;
        }
        return null;
    }

    protected String getK8sImplType() {
        List<Configuration> k8sImplTypeConfig =
                systemConfigService.queryByKeyPrefix("odc.task-framework.k8s-properties.k8s-impl-type");
        if (CollectionUtils.isEmpty(k8sImplTypeConfig)) {
            return AbstractK8sResourceOperatorBuilder.CLOUD_K8S_POD_TYPE;
        }
        return k8sImplTypeConfig.get(0).getValue();
    }

    protected boolean getK8sEnablePortMapper() {
        List<Configuration> enablePortMapperConfig =
                systemConfigService.queryByKeyPrefix("odc.task-framework.enable-k8s-port-mapper");
        if (CollectionUtils.isEmpty(enablePortMapperConfig)) {
            return false;
        }
        return Boolean.valueOf(enablePortMapperConfig.get(0).getValue());
    }

    protected K8sProperties buildK8sProperties(ConnectionConfig connectionConfig) {
        String prefix = "odc.task-framework.k8s-properties.";
        // default configuration list
        K8sProperties k8sProperties = new K8sProperties();
        // translate to map
        Map<String, String> oscK8sPropertiesMap = translateToMap(systemConfigService.queryByKeyPrefix(prefix));
        // set kube config, must required, only support config one k8s url
        if (StringUtils.isNotEmpty(oscK8sPropertiesMap.get(prefix + "kube-url"))) {
            trySetK8sPropertiesWithCheck(oscK8sPropertiesMap, prefix + "kube-url", k8sProperties::setKubeUrl);
        } else {
            trySetK8sPropertiesWithCheck(oscK8sPropertiesMap, prefix + "kube-config", k8sProperties::setKubeConfig);
        }
        // dependent configs, start with odc.osc.k8s-properties.
        prefix = "odc.osc.k8s-properties.";
        oscK8sPropertiesMap = translateToMap(systemConfigService.queryByKeyPrefix(prefix));
        trySetK8sPropertiesWithCheck(oscK8sPropertiesMap, prefix + "namespace", k8sProperties::setNamespace);
        trySetK8sPropertiesWithCheck(oscK8sPropertiesMap, prefix + "pod-image-name", k8sProperties::setPodImageName);
        // may with default
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "pod-pending-timeout-seconds", "3600",
                (v) -> k8sProperties.setPodPendingTimeoutSeconds(Long.valueOf(v)));
        // store 2cpu + writer 2 cpu
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "request-cpu", "4",
                (v) -> k8sProperties.setRequestCpu(Double.valueOf(v)));
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "limit-cpu", "4",
                (v) -> k8sProperties.setLimitCpu(Double.valueOf(v)));
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "node-cpu", "8",
                (v) -> k8sProperties.setNodeCpu(Double.valueOf(v)));

        // store 10g + writer 2g
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "request-mem", "12288",
                (v) -> k8sProperties.setRequestMem(Long.valueOf(v)));
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "limit-mem", "12288",
                (v) -> k8sProperties.setLimitMem(Long.valueOf(v)));
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "node-mem-in-mb", "16384",
                (v) -> k8sProperties.setNodeMemInMB(Long.valueOf(v)));

        // mount related
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "enable-mount", "false",
                (v) -> k8sProperties.setEnableMount(Boolean.valueOf(v)));
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "mount-path", "/var/log/odc-task/runtime",
                k8sProperties::setMountPath);
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "mount-disk-size", "64",
                (v) -> k8sProperties.setMountDiskSize(Long.valueOf(v)));
        trySetK8sProperties(oscK8sPropertiesMap, prefix + "max-node-count", "50",
                (v) -> k8sProperties.setMaxNodeCount(Long.valueOf(v)));
        k8sProperties.setSupervisorListenPort(18001);
        k8sProperties.setExecutorListenPort(null);
        // set region and group
        k8sProperties.setRegion(StringUtils.isEmpty(connectionConfig.getRegion()) ? ResourceIDUtil.DEFAULT_PROP_VALUE
                : connectionConfig.getRegion());
        k8sProperties
                .setGroup(StringUtils.isEmpty(connectionConfig.getCloudProvider()) ? ResourceIDUtil.DEFAULT_PROP_VALUE
                        : connectionConfig.getCloudProvider());
        return k8sProperties;
    }

    protected void trySetK8sProperties(Map<String, String> oscK8sPropertiesMap,
            String keyName, String defaultValue, Consumer<String> valueConsumer) {
        String value = oscK8sPropertiesMap.getOrDefault(keyName, defaultValue);
        valueConsumer.accept(value);
    }

    protected void trySetK8sPropertiesWithCheck(Map<String, String> oscK8sPropertiesMap,
            String keyName, Consumer<String> valueConsumer) {
        String value = oscK8sPropertiesMap.get(keyName);
        if (null == value) {
            throw new RuntimeException(keyName + " is required for osc k8s properties");
        }
        valueConsumer.accept(value);
    }

    protected Map<String, String> translateToMap(List<Configuration> configurations) {
        Map<String, String> ret = new LinkedHashMap<>();
        for (Configuration configuration : configurations) {
            ret.put(configuration.getKey(), configuration.getValue());
        }
        return ret;
    }
}
