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
package com.oceanbase.odc.metadb.worksheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.oceanbase.odc.core.shared.constant.ResourceType;
import com.oceanbase.odc.metadb.resourcehistory.ResourceLastAccessEntity;
import com.oceanbase.odc.metadb.resourcehistory.ResourceLastAccessRepository;
import com.oceanbase.odc.server.OdcServer;
import com.oceanbase.odc.service.resourcehistory.ResourceLastAccessService;
import com.oceanbase.odc.service.worksheet.domain.Path;

import cn.hutool.core.lang.Tuple;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OdcServer.class)
@EnableTransactionManagement
public class CollaborationWorksheetRepositoryTest {

    @Autowired
    private CollaborationWorksheetRepository collaborationWorksheetRepository;
    @Autowired
    private ResourceLastAccessService resourceLastAccessService;
    @Autowired
    private ResourceLastAccessRepository resourceLastAccessRepository;

    final long organizationId = 1L;
    final long projectId = 1L;
    long incrId = 0;
    long creatorId = 0;
    Long defaultVersion = 1L;

    private CollaborationWorksheetEntity newWorksheet(String path) {
        return newWorksheet(null, projectId, new Path(path), incrId++ + "", creatorId, defaultVersion);
    }

    private CollaborationWorksheetEntity newWorksheet(Long id, Long projectId, Path path, String objectId,
            Long creatorId, Long version) {
        return CollaborationWorksheetEntity.builder()
                .id(id)
                .projectId(projectId)
                .creatorId(creatorId)
                .path(path.getStandardPath())
                .pathLevel(path.getLevelNum())
                .objectId(objectId)
                .extension(path.getExtension())
                .size(0L)
                .version(version)
                .build();

    }

    @Before
    public void setup() {
        clear();
    }

    @After
    public void clear() {
        collaborationWorksheetRepository.deleteByProjectId(projectId);
        resourceLastAccessRepository.delete(ResourceLastAccessEntity.builder().organizationId(organizationId).build());
    }

    @Test
    public void findByProjectIdAndPath() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> worksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(worksheets);
        Optional<CollaborationWorksheetEntity> worksheet =
                collaborationWorksheetRepository.findByProjectIdAndPath(projectId,
                        "/Worksheets/folder1/folder4/");
        assertTrue(worksheet.isPresent());
        assertNotNull(worksheet.get().getId());

        worksheet =
                collaborationWorksheetRepository.findByProjectIdAndPath(projectId,
                        "/Worksheets/folder1/folder3");
        assertFalse(worksheet.isPresent());
    }

    @Test
    public void findByProjectIdAndInPaths() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> addWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(addWorksheets);
        List<CollaborationWorksheetEntity> worksheets =
                collaborationWorksheetRepository.findByProjectIdAndInPaths(projectId,
                        Arrays.asList("/Worksheets/folder1/folder4/", "/Worksheets/folder1/folder3",
                                "/Worksheets/file1.sql"));
        assertPathEquals(worksheets, Arrays.asList(w3, w6));
    }

    @Test
    public void findWithPathLike_Normal() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> addWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(addWorksheets);
        List<CollaborationWorksheetEntity> worksheets =
                collaborationWorksheetRepository.findByPathLikeWithFilter(projectId,
                        "/Worksheets/folder1/", null,
                        null, null);
        assertPathEquals(worksheets, Arrays.asList(w1, w2, w3, w4));
        assertEquals(worksheets.size(), collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", null,
                null, null));
    }

    @Test
    public void findWithPathLike_LevelNumFilter() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> addWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(addWorksheets);
        List<CollaborationWorksheetEntity> worksheets =
                collaborationWorksheetRepository.findByPathLikeWithFilter(projectId,
                        "/Worksheets/folder1/",
                        3, 3, null);
        assertPathEquals(worksheets, Arrays.asList(w2, w3));
        assertEquals(worksheets.size(), collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", 3, 3, null));

        worksheets =
                collaborationWorksheetRepository.findByPathLikeWithFilter(projectId,
                        "/Worksheets/folder1/",
                        1, 4, null);
        assertPathEquals(worksheets, Arrays.asList(w1, w2, w3, w4));
        assertEquals(worksheets.size(), collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", 1, 4, null));


        worksheets =
                collaborationWorksheetRepository.findByPathLikeWithFilter(projectId,
                        "/Worksheets/folder1/",
                        4, 2, null);
        assertPathEquals(worksheets, Arrays.asList(w1, w2, w3, w4));
        assertEquals(worksheets.size(), collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", 4, 2, null));
    }

    @Test
    public void findWithPathLike_NameLikeFilter() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> addWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(addWorksheets);
        List<CollaborationWorksheetEntity> worksheets =
                collaborationWorksheetRepository.findByPathLikeWithFilter(projectId,
                        "/Worksheets/folder1/",
                        1, 4, "le2");
        assertPathEquals(worksheets, Collections.singletonList(w2));
        assertEquals(worksheets.size(), collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", 1, 4, "le2"));
    }

    @Test
    public void testLeftJoinResourceLastAccess_SortByUpdateTime_FirstPage() {
        int totalSize = 19;
        int pageSize = 10;
        PageRequest pageRequest =
                PageRequest.of(0, pageSize, Sort.by(Direction.DESC, "updateTime"));

        Tuple tuple = prepareTestDateForLeftJoinResourceLastAccess(totalSize);
        List<Long> exceptFirstPageListIdsOrderByUpdateTimeDesc =
                tuple.<List<Long>>get(0).subList((int) pageRequest.getOffset(), pageSize);

        Page<CollaborationWorksheetEntity> result = collaborationWorksheetRepository
                .leftJoinResourceLastAccess(organizationId, projectId, creatorId, pageRequest);

        assertEquals(exceptFirstPageListIdsOrderByUpdateTimeDesc,
                result.getContent().stream().map(CollaborationWorksheetEntity::getId)
                        .collect(Collectors.toList()));
    }

    @Test
    public void testLeftJoinResourceLastAccess_SortByUpdateTime_LastPage() {
        int totalSize = 19;
        int pageSize = 10;
        PageRequest pageRequest =
                PageRequest.of(lastPage(totalSize, pageSize), pageSize, Sort.by(Direction.DESC, "updateTime"));

        Tuple tuple = prepareTestDateForLeftJoinResourceLastAccess(totalSize);
        List<Long> exceptFirstPageListIdsOrderByUpdateTimeDesc =
                tuple.<List<Long>>get(0).subList((int) pageRequest.getOffset(), totalSize);

        Page<CollaborationWorksheetEntity> result = collaborationWorksheetRepository
                .leftJoinResourceLastAccess(organizationId, projectId, creatorId, pageRequest);

        assertEquals(exceptFirstPageListIdsOrderByUpdateTimeDesc,
                result.getContent().stream().map(CollaborationWorksheetEntity::getId).collect(
                        Collectors.toList()));
    }

    @Test
    public void testLeftJoinResourceLastAccess_SortByLastAccessTime_FirstPage() {
        int totalSize = 19;
        int pageSize = 10;
        PageRequest pageRequest =
                PageRequest.of(0, pageSize, Sort.by(Direction.DESC, "lastAccessTime"));

        Tuple tuple = prepareTestDateForLeftJoinResourceLastAccess(totalSize);
        List<Long> exceptFirstPageListIdsOrderByLastAccessTimeDesc =
                tuple.<List<Long>>get(1).subList((int) pageRequest.getOffset(), pageSize);

        Page<CollaborationWorksheetEntity> result = collaborationWorksheetRepository
                .leftJoinResourceLastAccess(organizationId, projectId, creatorId, pageRequest);

        assertEquals(exceptFirstPageListIdsOrderByLastAccessTimeDesc,
                result.getContent().stream().map(CollaborationWorksheetEntity::getId)
                        .collect(Collectors.toList()));
    }

    @Test
    public void testLeftJoinResourceLastAccess_SortByLastAccessTime_LastPage() {
        int totalSize = 19;
        int pageSize = 10;
        PageRequest pageRequest =
                PageRequest.of(lastPage(totalSize, pageSize), pageSize, Sort.by(Direction.DESC, "lastAccessTime"));

        Tuple tuple = prepareTestDateForLeftJoinResourceLastAccess(totalSize);
        List<Long> exceptFirstPageListIdsOrderByLastAccessTimeDesc =
                tuple.<List<Long>>get(1).subList((int) pageRequest.getOffset(), totalSize);

        Page<CollaborationWorksheetEntity> result = collaborationWorksheetRepository
                .leftJoinResourceLastAccess(organizationId, projectId, creatorId, pageRequest);

        assertEquals(exceptFirstPageListIdsOrderByLastAccessTimeDesc,
                result.getContent().stream().map(CollaborationWorksheetEntity::getId)
                        .collect(Collectors.toList()));
    }

    @Test
    public void batchAdd() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> addWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(addWorksheets);
        for (CollaborationWorksheetEntity worksheet : addWorksheets) {
            Optional<CollaborationWorksheetEntity> byId = collaborationWorksheetRepository.findById(worksheet.getId());
            assertTrue(byId.isPresent());
        }
    }

    @Test
    public void batchAdd_ViolationUniqueConstraints() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> addWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(addWorksheets);

        CollaborationWorksheetEntity w7 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Set<CollaborationWorksheetEntity> addWorksheets2 = new HashSet<>(Arrays.asList(w5, w7));
        assertThrows(DataIntegrityViolationException.class,
                () -> collaborationWorksheetRepository.saveAllAndFlush(addWorksheets2));
    }

    @Test
    public void batchDeleteByIds() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> worksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(worksheets);

        collaborationWorksheetRepository.deleteAllByIdInBatch(
                Stream.of(w4, w5, w6).map(CollaborationWorksheetEntity::getId).collect(Collectors.toList()));

        for (CollaborationWorksheetEntity worksheet : Arrays.asList(w4, w5, w6)) {
            Optional<CollaborationWorksheetEntity> byId = collaborationWorksheetRepository.findById(worksheet.getId());
            assertFalse(byId.isPresent());
        }
    }

    @Test

    public void batchDeleteByPathLike() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> worksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(worksheets);

        int count = collaborationWorksheetRepository.batchDeleteByPathLike(projectId, "/Worksheets/folder1/");
        assertEquals(count, 4);

        for (CollaborationWorksheetEntity worksheet : Arrays.asList(w1, w2, w3, w4)) {
            Optional<CollaborationWorksheetEntity> byId = collaborationWorksheetRepository.findById(worksheet.getId());
            assertFalse(byId.isPresent());
        }
    }

    @Test
    public void updateContentByIdAndVersion() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> worksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(worksheets);

        w2.setObjectId(incrId++ + "");
        int count = collaborationWorksheetRepository.updateContentByIdAndVersion(w2);
        assert count == 1;

        Optional<CollaborationWorksheetEntity> byId = collaborationWorksheetRepository.findById(w2.getId());
        assertTrue(byId.isPresent());
        assertEquals(byId.get().getObjectId(), w2.getObjectId());
        assertEquals(byId.get().getVersion(), new Long(w2.getVersion() + 1L));

        w4.setObjectId(incrId++ + "");
        w4.setVersion(1L);
        count = collaborationWorksheetRepository.updateContentByIdAndVersion(w2);
        assertEquals(count, 0);
        byId = collaborationWorksheetRepository.findById(w4.getId());
        assertTrue(byId.isPresent());
        assertNotEquals(byId.get().getObjectId(), w2.getObjectId());
        assertEquals(byId.get().getVersion(), new Long(1L));
    }

    @Test
    public void batchUpdatePath() {
        CollaborationWorksheetEntity w1 = newWorksheet("/Worksheets/folder1/");
        CollaborationWorksheetEntity w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        CollaborationWorksheetEntity w3 = newWorksheet("/Worksheets/folder1/folder4/");
        CollaborationWorksheetEntity w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        CollaborationWorksheetEntity w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        CollaborationWorksheetEntity w6 = newWorksheet("/Worksheets/file1.sql");
        Set<CollaborationWorksheetEntity> worksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        collaborationWorksheetRepository.saveAllAndFlush(worksheets);
        Map<Long, String> updateMap = new HashMap<>();
        updateMap.put(w1.getId(), "/Worksheets/folder10/");
        updateMap.put(w2.getId(), "/Worksheets/folder1/file20.sql");
        updateMap.put(w3.getId(), "/Worksheets/folder1/folder40/");
        int count =
                collaborationWorksheetRepository.batchUpdatePath(updateMap);
        assertEquals(count, 3);

        Map<Long, CollaborationWorksheetEntity> idToEnityMap = worksheets.stream().collect(
                Collectors.toMap(CollaborationWorksheetEntity::getId, Function.identity()));
        for (Long id : updateMap.keySet()) {
            CollaborationWorksheetEntity update = collaborationWorksheetRepository.findById(id).get();
            assertEquals(update.getPath(), updateMap.get(update.getId()));
            assertEquals(update.getObjectId(), idToEnityMap.get(update.getId()).getObjectId());
            assertEquals(update.getVersion(), idToEnityMap.get(update.getId()).getVersion());
        }
    }

    private void assertPathEquals(Collection<CollaborationWorksheetEntity> c1,
            Collection<CollaborationWorksheetEntity> c2) {
        assertEquals(c1.size(), c2.size());
        assertEquals(c1.stream().map(CollaborationWorksheetEntity::getPath).collect(Collectors.toSet()),
                c2.stream().map(CollaborationWorksheetEntity::getPath).collect(Collectors.toSet()));
    }

    private int lastPage(int totalSize, int pageSize) {
        return totalSize % pageSize == 0 ? totalSize / pageSize - 1 : totalSize / pageSize;
    }

    private Tuple prepareTestDateForLeftJoinResourceLastAccess(int totalSize) {
        long updateTs = 1L;

        Set<CollaborationWorksheetEntity> worksheets = new HashSet<>();
        for (int i = 0; i < totalSize; i++) {
            CollaborationWorksheetEntity worksheet = newWorksheet("/Worksheets/folder1/file" + i + ".sql");
            worksheet.setUpdateTime(new Date(updateTs + i * 1000L));
            worksheets.add(worksheet);
        }
        collaborationWorksheetRepository.saveAllAndFlush(worksheets);
        List<Long> listIdsOrderByUpdateTimeDesc =
                worksheets.stream().sorted(Comparator.comparing(CollaborationWorksheetEntity::getUpdateTime).reversed())
                        .map(CollaborationWorksheetEntity::getId).collect(Collectors.toList());

        long lastAccessTs = System.currentTimeMillis();
        int i = 0;
        List<Long> listIdsOrderByLastAccessDesc = new ArrayList<>();
        List<Long> listIdsWithLastAccessIsNull = new ArrayList<>();
        while (i < listIdsOrderByUpdateTimeDesc.size()) {
            resourceLastAccessService.add(organizationId, projectId, creatorId,
                    ResourceType.ODC_WORKSHEET, listIdsOrderByUpdateTimeDesc.get(i), new Date(lastAccessTs - i * 1000));
            listIdsOrderByLastAccessDesc.add(listIdsOrderByUpdateTimeDesc.get(i));
            if (i + 1 < listIdsOrderByUpdateTimeDesc.size() - 1) {
                listIdsWithLastAccessIsNull.add(listIdsOrderByUpdateTimeDesc.get(i + 1));
            }
            i = i + 2;
        }
        // Due to some worksheet users not having accessed it, there is no last access time.
        // When the recent last time is null, it will be sorted in descending order based on the id.
        listIdsOrderByLastAccessDesc.addAll(listIdsWithLastAccessIsNull.stream()
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList()));

        return new Tuple(listIdsOrderByUpdateTimeDesc, listIdsOrderByLastAccessDesc);
    }
}