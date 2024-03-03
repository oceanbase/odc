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
package com.oceanbase.odc.metadb.flow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;

import com.oceanbase.odc.ServiceTestEnv;
import com.oceanbase.odc.core.flow.model.FlowableElementType;
import com.oceanbase.odc.service.flow.model.FlowNodeStatus;
import com.oceanbase.odc.service.flow.model.FlowNodeType;

/**
 * Test cases for {@link UserTaskInstanceRepository}
 *
 * @author yh263208
 * @date 2022-02-07 16:15
 * @since ODC_release_3.3.0
 * @see ServiceTestEnv
 */
public class UserTaskInstanceRepositoryTest extends ServiceTestEnv {

    @Autowired
    private UserTaskInstanceRepository repository;
    private final Random random = new Random();
    @Autowired
    private NodeInstanceEntityRepository nodeInstanceEntityRepository;
    @Autowired
    private UserTaskInstanceCandidateRepository candidateRepository;

    @Before
    public void setUp() {
        candidateRepository.deleteAll();
        repository.deleteAll();
    }

    @After
    public void clearAll() {
        candidateRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    public void testSaveUserTaskInstanceEntity() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(getById(entity.getId()), entity);
    }

    @Test
    public void bulkSave_saveSeveralEntities_saveSucceed() {
        List<UserTaskInstanceEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            entities.add(createEntity());
        }
        repository.batchCreate(entities);
        Assert.assertEquals(10, repository.findAll().size());
    }

    @Test
    public void save_ApprovalTaskAndNodeInstanceEntity_ExpectGetObjectByActivityId() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);

        NodeInstanceEntity nodeInstanceEntity = createNodeEntity(entity.getFlowInstanceId(), entity.getId());
        Optional<UserTaskInstanceEntity> optional = repository.findByInstanceTypeAndActivityId(
                FlowNodeType.APPROVAL_TASK, nodeInstanceEntity.getActivityId(), entity.getFlowInstanceId());
        Assert.assertTrue(optional.isPresent());
    }

    @Test
    public void save_ApprovalTaskAndNodeInstanceEntity_ExpectGetObjectByName() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);

        NodeInstanceEntity nodeInstanceEntity = createNodeEntity(entity.getFlowInstanceId(), entity.getId());
        Optional<UserTaskInstanceEntity> optional = repository.findByInstanceTypeAndName(FlowNodeType.APPROVAL_TASK,
                nodeInstanceEntity.getName(), entity.getFlowInstanceId());
        Assert.assertTrue(optional.isPresent());
    }

    @Test
    public void testDeleteUserTaskInstanceEntity() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        repository.delete(entity);
        Assert.assertEquals(repository.count(), 0);
    }

    @Test
    public void testDeleteUserTaskInstanceEntityByinstanceId() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);
        Assert.assertEquals(1, repository.deleteByFlowInstanceId(entity.getFlowInstanceId()));
        Assert.assertEquals(repository.count(), 0);
    }

    @Test
    public void testQueryBySpecification() {
        Long[] ids = insertBatch(100);
        Long id = ids[0];
        Specification<UserTaskInstanceEntity> specification = Specification.where(UserTaskInstanceSpecs.idEquals(id));
        List<UserTaskInstanceEntity> entities = repository.findAll(specification);
        Assert.assertEquals(entities.size(), 1);
        Optional<UserTaskInstanceEntity> optional = repository.findById(id);
        Assert.assertEquals(optional.orElse(null), entities.get(0));
    }

    @Test
    public void testPagedFindAll() {
        insertBatch(100);
        Specification<UserTaskInstanceEntity> specification =
                Specification.where(UserTaskInstanceSpecs.disApprovaled());
        Page<UserTaskInstanceEntity> entities =
                repository.findAll(specification, PageRequest.of(1, 15, Sort.by(Direction.ASC, "createTime")));
        Assert.assertEquals(entities.getTotalElements(), 100);
        Assert.assertEquals(entities.getSize(), 15);
    }

    @Test
    public void testPagedFindAll_1() {
        insertBatch(100);
        Specification<UserTaskInstanceEntity> specification = Specification.where(UserTaskInstanceSpecs.approvaled());
        Page<UserTaskInstanceEntity> entities =
                repository.findAll(specification, PageRequest.of(1, 15, Sort.by(Direction.ASC, "createTime")));
        Assert.assertEquals(entities.getTotalElements(), 0);
        Assert.assertEquals(entities.getSize(), 15);
    }

    @Test
    public void testUpdateUserTaskInstanceById() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        entity.setStatus(FlowNodeStatus.FAILED);
        entity.setUserTaskId(UUID.randomUUID().toString());
        entity.setApproved(!entity.isApproved());
        entity.setComment("lalalalal");
        entity.setOperatorId(random.nextLong());
        Assert.assertEquals(1, repository.update(entity));

        Optional<UserTaskInstanceEntity> newEntity = repository.findById(entity.getId());
        Assert.assertTrue(newEntity.isPresent());
        Assert.assertEquals(entity, newEntity.get());
    }

    @Test
    public void testUpdateUserTaskIdByInstanceById() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);

        String userTaskId = UUID.randomUUID().toString();
        Assert.assertEquals(1, repository.updateUserTaskIdById(entity.getId(), userTaskId));

        Optional<UserTaskInstanceEntity> newEntity = repository.findById(entity.getId());
        Assert.assertTrue(newEntity.isPresent());
        Assert.assertEquals(userTaskId, newEntity.get().getUserTaskId());
    }

    @Test
    public void testUpdateStatusByInstanceById() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);
        Assert.assertEquals(repository.count(), 1);

        Assert.assertEquals(1, repository.updateStatusById(entity.getId(), FlowNodeStatus.COMPLETED));

        Optional<UserTaskInstanceEntity> newEntity = repository.findById(entity.getId());
        Assert.assertTrue(newEntity.isPresent());
        Assert.assertEquals(FlowNodeStatus.COMPLETED, newEntity.get().getStatus());
    }

    @Test
    public void save_UserTaskInstanceCandidate_SuccessCreated() {
        long instanceId = random.nextLong();
        UserTaskInstanceCandidateEntity entity = createEntity(instanceId, true);
        candidateRepository.save(entity);

        UserTaskInstanceCandidateEntity entity1 = createEntity(instanceId + 10, false);
        candidateRepository.save(entity1);

        List<UserTaskInstanceCandidateEntity> entities = candidateRepository.findByApprovalInstanceId(instanceId);
        Assert.assertEquals(1, entities.size());
        Assert.assertEquals(entity, entities.get(0));
    }

    @Test
    public void delete_UserTaskInstanceCandidate_SucceedDelete() {
        long instanceId = random.nextLong();
        UserTaskInstanceCandidateEntity entity = createEntity(instanceId, true);
        candidateRepository.save(entity);

        entity = createEntity(instanceId, false);
        candidateRepository.save(entity);

        Assert.assertEquals(2, candidateRepository.deleteByApprovalInstanceId(instanceId));
    }

    @Test
    public void delete_UserTaskInstanceCandidateByUserId_SucceedDelete() {
        long instanceId = random.nextLong();
        UserTaskInstanceCandidateEntity entity = createEntity(instanceId, true);
        candidateRepository.save(entity);

        Assert.assertEquals(1, candidateRepository.deleteByApprovalInstanceIdAndUserId(instanceId, entity.getUserId()));
    }

    @Test
    public void delete_UserTaskInstanceCandidateByRoleId_SucceedDelete() {
        long instanceId = random.nextLong();
        List<Long> roleIds = new LinkedList<>();
        UserTaskInstanceCandidateEntity entity = createEntity(instanceId, false);
        roleIds.add(entity.getRoleId());
        candidateRepository.save(entity);

        entity = createEntity(instanceId, false);
        roleIds.add(entity.getRoleId());
        candidateRepository.save(entity);

        Assert.assertEquals(2, candidateRepository.deleteByApprovalInstanceIdAndRoleIds(instanceId, roleIds));
    }

    @Test
    public void find_UserTaskInstanceByUserIdAndRoleIds_Success_Found() {
        UserTaskInstanceEntity entity = createEntity();
        repository.save(entity);

        UserTaskInstanceCandidateEntity candidateEntity = createEntity(entity.getId(), random.nextBoolean());
        candidateRepository.save(candidateEntity);

        List<UserTaskInstanceEntity> entities = repository.findByCandidateUserIdOrRoleIds(candidateEntity.getUserId(),
                Collections.singletonList(candidateEntity.getRoleId()));
        Assert.assertNotNull(entities);
        Assert.assertFalse(entities.isEmpty());
        Assert.assertEquals(entities.get(0), entity);
    }

    private UserTaskInstanceCandidateEntity createEntity(Long approvalInstanceId, boolean userId) {
        UserTaskInstanceCandidateEntity entity = new UserTaskInstanceCandidateEntity();
        entity.setApprovalInstanceId(approvalInstanceId);
        if (userId) {
            entity.setUserId(random.nextLong());
        } else {
            entity.setRoleId(random.nextLong());
        }
        return entity;
    }

    private NodeInstanceEntity createNodeEntity(Long flowInstanceId, Long instanceId) {
        NodeInstanceEntity entity = new NodeInstanceEntity();
        entity.setFlowInstanceId(flowInstanceId);
        entity.setInstanceId(instanceId);
        entity.setInstanceType(FlowNodeType.APPROVAL_TASK);
        entity.setActivityId(UUID.randomUUID().toString());
        entity.setName(UUID.randomUUID().toString());
        entity.setFlowableElementType(FlowableElementType.USER_TASK);
        entity = nodeInstanceEntityRepository.save(entity);
        return entity;
    }

    private UserTaskInstanceEntity createEntity() {
        UserTaskInstanceEntity entity = new UserTaskInstanceEntity();
        entity.setOperatorId(123L);
        entity.setComment("Comment test");
        entity.setApproved(false);
        entity.setFlowInstanceId(random.nextLong());
        entity.setOrganizationId(1L);
        entity.setUserTaskId("test_user_task_id");
        entity.setStatus(FlowNodeStatus.CREATED);
        entity.setExpireIntervalSeconds(12);
        return entity;
    }

    private UserTaskInstanceEntity getById(Long id) {
        Optional<UserTaskInstanceEntity> optional = repository.findById(id);
        return optional.orElse(null);
    }

    private Long[] insertBatch(int batchSize) {
        Long[] ids = new Long[batchSize];
        for (int i = 0; i < batchSize; i++) {
            UserTaskInstanceEntity entity = createEntity();
            repository.save(entity);
            ids[i] = entity.getId();
        }
        return ids;
    }
}
