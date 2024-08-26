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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.oceanbase.odc.server.OdcServer;
import com.oceanbase.odc.service.worksheet.domain.Path;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OdcServer.class)
@EnableTransactionManagement
public class CollaborationWorksheetRepositoryTest {

    @Autowired
    private CollaborationWorksheetRepository collaborationWorksheetRepository;


    final long projectId = System.currentTimeMillis();
    long incrId = 0;
    long creatorId = 0;
    Long defaultVersion = 1L;

    private CollaborationWorksheetEntity newWorksheet(String path) {
        return newWorksheet(null, projectId, new Path(path), incrId++ + "", creatorId, defaultVersion);
    }

    private CollaborationWorksheetEntity newWorksheet(String path, String objectId) {
        return newWorksheet(null, projectId, new Path(path), objectId, creatorId, defaultVersion);
    }

    private CollaborationWorksheetEntity newWorksheet(Long id, Long projectId, Path path, String objectId,
            Long creatorId, Long version) {
        return CollaborationWorksheetEntity.builder()
                .id(id)
                .projectId(projectId)
                .creatorId(creatorId)
                .path(path.getStandardPath())
                .levelNum(path.getLevelNum())
                .objectId(objectId)
                .extension(path.getExtension())
                .totalLength(0L)
                .version(version)
                .build();

    }

    @After
    public void clear() {
        collaborationWorksheetRepository.deleteByProjectId(projectId);
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
        assert worksheet.isPresent();
        assert worksheet.get().getId() != null;

        worksheet =
                collaborationWorksheetRepository.findByProjectIdAndPath(projectId,
                        "/Worksheets/folder1/folder3");
        assert !worksheet.isPresent();
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
        assert CollectionUtils.isNotEmpty(worksheets);
        assert worksheets.size() == 2;
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
        assert !worksheets.isEmpty();
        assert worksheets.size() == 4;
        assert worksheets.size() == collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", null,
                null, null);
        assertPathEquals(worksheets, Arrays.asList(w1, w2, w3, w4));
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
        assert !worksheets.isEmpty();
        assert worksheets.size() == 2;
        assert worksheets.size() == collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", 3, 3, null);
        assertPathEquals(worksheets, Arrays.asList(w2, w3));

        worksheets =
                collaborationWorksheetRepository.findByPathLikeWithFilter(projectId,
                        "/Worksheets/folder1/",
                        1, 4, null);
        assert !worksheets.isEmpty();
        assert worksheets.size() == 4;
        assert worksheets.size() == collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", 1, 4, null);
        assertPathEquals(worksheets, Arrays.asList(w1, w2, w3, w4));

        worksheets =
                collaborationWorksheetRepository.findByPathLikeWithFilter(projectId,
                        "/Worksheets/folder1/",
                        4, 2, null);
        assert !worksheets.isEmpty();
        assert worksheets.size() == 4;
        assert worksheets.size() == collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", 4, 2, null);
        assertPathEquals(worksheets, Arrays.asList(w1, w2, w3, w4));
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
        assert !worksheets.isEmpty();
        assert worksheets.size() == 1;
        assert worksheets.size() == collaborationWorksheetRepository.countByPathLikeWithFilter(projectId,
                "/Worksheets/folder1/", 1, 4, "le2");
        assertPathEquals(worksheets, Arrays.asList(w2));
    }

    private void assertPathEquals(Collection<CollaborationWorksheetEntity> c1,
            Collection<CollaborationWorksheetEntity> c2) {
        assertTrue(c1.size() == c2.size());
        assertEquals(c1.stream().map(CollaborationWorksheetEntity::getPath).collect(Collectors.toSet()),
                c2.stream().map(CollaborationWorksheetEntity::getPath).collect(Collectors.toSet()));
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
        assert count == 4;

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
        assert count == 0;
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
        assert count == 3;

        Map<Long, CollaborationWorksheetEntity> idToEnityMap = worksheets.stream().collect(
                Collectors.toMap(CollaborationWorksheetEntity::getId, Function.identity()));
        for (Long id : updateMap.keySet()) {
            CollaborationWorksheetEntity update = collaborationWorksheetRepository.findById(id).get();
            assertEquals(update.getPath(), updateMap.get(update.getId()));
            assertEquals(update.getObjectId(), idToEnityMap.get(update.getId()).getObjectId());
            assertEquals(update.getVersion(), idToEnityMap.get(update.getId()).getVersion());
        }
    }

}
