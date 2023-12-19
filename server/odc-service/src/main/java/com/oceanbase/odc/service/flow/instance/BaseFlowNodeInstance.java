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
package com.oceanbase.odc.service.flow.instance;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;

import com.oceanbase.odc.core.authority.model.SecurityResource;
import com.oceanbase.odc.core.flow.graph.GraphVertex;
import com.oceanbase.odc.core.flow.model.FlowableElement;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.core.shared.OrganizationIsolated;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntity;
import com.oceanbase.odc.metadb.flow.NodeInstanceEntityRepository;
import com.oceanbase.odc.metadb.flow.SequenceInstanceRepository;
import com.oceanbase.odc.service.flow.FlowableAdaptor;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link BaseFlowNodeInstance} object in process instance, used to represent approval task
 * instance, user task instance, etc.
 *
 * @author yh263208
 * @date 2022-02-10 14:45
 * @since ODC_release_3.3.0
 */
@Slf4j
@Getter
public abstract class BaseFlowNodeInstance extends GraphVertex implements SecurityResource, OrganizationIsolated {
    @Setter
    private volatile FlowNodeStatus status;
    @Setter
    protected Long id;
    protected Date createTime;
    protected Date updateTime;
    private final boolean endEndPoint;
    private final boolean startEndpoint;
    private final FlowNodeType nodeType;
    private final long organizationId;
    private final long flowInstanceId;
    private final List<FlowableElement> bindFlowableElements;
    private final transient String shortUniqueId;
    @Setter
    private String activityId;

    @Getter(AccessLevel.NONE)
    protected final FlowableAdaptor flowableAdaptor;
    @Getter(AccessLevel.NONE)
    private final NodeInstanceEntityRepository nodeRepository;
    @Getter(AccessLevel.NONE)
    private final SequenceInstanceRepository sequenceRepository;

    /**
     * Default constructor of a flow instance node, It is used to read from the database. At this time,
     * fields such as {@link BaseFlowNodeInstance#getId()} and
     * {@link BaseFlowNodeInstance#getCreateTime()} exist and need to be filled in
     *
     * @param id Id for this instance, standard for id in database
     * @param nodeType type for this flow node instance
     * @param organizationId organization id
     * @param flowInstanceId related flow instance
     * @param createTime create time
     * @param updateTime update time
     */
    protected BaseFlowNodeInstance(@NonNull FlowNodeType nodeType, Long id,
            @NonNull Long organizationId, @NonNull FlowNodeStatus status,
            @NonNull Long flowInstanceId, Date createTime, Date updateTime, boolean startEndpoint, boolean endEndPoint,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository) {
        super(UUID.randomUUID().toString(), null);
        this.nodeType = nodeType;
        this.id = id;
        this.organizationId = organizationId;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.flowInstanceId = flowInstanceId;
        this.startEndpoint = startEndpoint;
        this.endEndPoint = endEndPoint;
        this.nodeRepository = nodeRepository;
        this.sequenceRepository = sequenceRepository;
        this.status = status;
        this.flowableAdaptor = flowableAdaptor;
        if (this.id != null) {
            FlowableElement coreElement = getCoreFlowableElement();
            setName(coreElement.getName());
            this.activityId = coreElement.getActivityId();
        } else {
            this.activityId = null;
        }
        this.bindFlowableElements = new ArrayList<>();
        this.shortUniqueId = RandomStringUtils.random(12, UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * It is used to initialize an instance from the configuration. At this time, fields such as
     * {@link BaseFlowNodeInstance#getId()} and {@link BaseFlowNodeInstance#getCreateTime()} are all
     * {@code null}, which need to be persisted to the database before they can be obtained.
     */
    protected BaseFlowNodeInstance(@NonNull FlowNodeType nodeType, @NonNull Long organizationId,
            @NonNull Long flowInstanceId, boolean startEndpoint, boolean endEndPoint,
            @NonNull FlowableAdaptor flowableAdaptor,
            @NonNull NodeInstanceEntityRepository nodeRepository,
            @NonNull SequenceInstanceRepository sequenceRepository) {
        this(nodeType, null, organizationId, FlowNodeStatus.CREATED, flowInstanceId, null, null,
                startEndpoint, endEndPoint, flowableAdaptor, nodeRepository, sequenceRepository);
    }

    @Override
    public String resourceId() {
        return getId() + "";
    }

    @Override
    public Long organizationId() {
        return getOrganizationId();
    }

    @Override
    public Long id() {
        return getId();
    }

    public void bindFlowableElement(@NonNull FlowableElement element) {
        this.bindFlowableElements.add(element);
    }

    /**
     * Delete instance information
     *
     * @return Delete operation result
     */
    public boolean delete() {
        dealloc();
        validExists();
        doDelete(getId());
        FlowableElementType coreType = getCoreFlowableElementType();
        List<NodeInstanceEntity> nodeInstanceEntities =
                nodeRepository.findByInstanceIdAndInstanceTypeAndFlowableElementType(getId(),
                        getNodeType(), coreType);
        if (nodeInstanceEntities.size() >= 2) {
            log.warn("Duplicate records are found, id={}, nodeType={}, coreType={} ", this.id, this.nodeType, coreType);
            throw new IllegalStateException("Duplicate records are found");
        }
        int affectRows = nodeRepository.deleteByInstanceIdAndInstanceType(getId(), getNodeType());
        if (log.isDebugEnabled()) {
            log.debug("Process node instance deletion completed, affectRows={}, id={}, instanceType={}", affectRows,
                    getId(), getNodeType());
        }
        boolean returnVal = true;
        if (affectRows == 0 && !nodeInstanceEntities.isEmpty()) {
            returnVal = false;
            log.warn("Deleting a node instance results not as expected, affectRows={}, id={}, instanceType={}",
                    affectRows, getId(), getNodeType());
        }
        if (nodeInstanceEntities.isEmpty()) {
            return true;
        }
        NodeInstanceEntity entity = nodeInstanceEntities.get(0);
        affectRows = sequenceRepository.deleteByNodeInstanceId(entity.getId());
        if (log.isDebugEnabled()) {
            log.debug("Sequence node instance deletion completed, affectRows={}, id={}, instanceType={}", affectRows,
                    getId(), getNodeType());
        }
        return returnVal;
    }

    protected void validExists() {
        if (getId() == null) {
            throw new NullPointerException("Id of this instance can not be null");
        }
    }

    protected void validNotExists() {
        if (getId() != null) {
            throw new IllegalStateException("Cannot create an already existing instance, Id exists");
        }
    }

    public FlowableElement getCoreFlowableElement() {
        if (this.id == null) {
            throw new IllegalStateException("Id is null");
        }
        FlowableElementType coreType = getCoreFlowableElementType();
        List<FlowableElement> flowableElements =
                this.flowableAdaptor.getFlowableElementByType(this.id, this.nodeType, coreType);
        if (CollectionUtils.isEmpty(flowableElements)) {
            throw new IllegalStateException("No flowable element is found by id " + this.id);
        }
        if (flowableElements.size() >= 2) {
            log.warn("Duplicate records are found, id={}, nodeType={}, coreType={} ", this.id, this.nodeType, coreType);
            throw new IllegalStateException("Duplicate records are found");
        }
        return flowableElements.get(0);
    }

    /**
     * Create instance information
     */
    abstract public void create();

    /**
     * Update instance information
     */
    abstract public void update();

    /**
     * Delete instance information
     */
    abstract protected void doDelete(@NonNull Long instanceId);

    /**
     * Method to indicate whether this node instance presents on this machine
     *
     * @return flag
     */
    abstract public boolean isPresentOnThisMachine();

    /**
     * dealloc resource
     */
    abstract public void dealloc();

    /**
     * dealloc resource
     */
    abstract protected void alloc();

    abstract public FlowableElementType getCoreFlowableElementType();

}
