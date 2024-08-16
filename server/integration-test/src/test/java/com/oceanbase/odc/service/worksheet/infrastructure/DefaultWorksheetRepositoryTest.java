/*
 * Copyright (c) 2024 OceanBase.
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

package com.oceanbase.odc.service.worksheet.infrastructure;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import com.oceanbase.odc.core.shared.exception.BadArgumentException;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataEntity;
import com.oceanbase.odc.metadb.objectstorage.ObjectMetadataRepository;
import com.oceanbase.odc.server.OdcServer;
import com.oceanbase.odc.service.worksheet.domain.Path;
import com.oceanbase.odc.service.worksheet.domain.Worksheet;
import com.oceanbase.odc.service.worksheet.utils.WorksheetUtil;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OdcServer.class)
@EnableTransactionManagement
public class DefaultWorksheetRepositoryTest {

    @Autowired
    private DefaultWorksheetRepository defaultWorksheetRepository;

    @Autowired
    private ObjectMetadataRepository metadataRepository;

    final long projectId = System.currentTimeMillis();
    long id = 0L;
    Long defaultVersion = 1L;

    private Worksheet newWorksheet(String path) {
        return newWorksheet(null, projectId, new Path(path), id++ + "", 1L);
    }

    public Worksheet newWorksheet(Long id, Long projectId, Path path, String objectId, Long creatorId) {
        return new Worksheet(id, new Date(), new Date(), projectId, path, creatorId,
                defaultVersion, objectId, null, null);
    }

    @After
    public void clear() {
        deleteByProjectId(projectId);
    }

    private void deleteByProjectId(Long projectId) {
        CriteriaBuilder criteriaBuilder = metadataRepository.getEntityManager().getCriteriaBuilder();
        CriteriaDelete<ObjectMetadataEntity> criteriaDelete =
                criteriaBuilder.createCriteriaDelete(ObjectMetadataEntity.class);
        Root<ObjectMetadataEntity> root = criteriaDelete.from(ObjectMetadataEntity.class);

        criteriaDelete.where(
                criteriaBuilder.equal(root.get("bucketName"), WorksheetUtil.getBucketNameOfWorkSheets(projectId)));

        metadataRepository.getEntityManager()
                .createQuery(criteriaDelete).executeUpdate();
    }

    @Test
    @Transactional
    public void testFindByProjectIdAndPath_List() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        defaultWorksheetRepository.batchAdd(subWorksheets);
        Optional<Worksheet> worksheet = defaultWorksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/folder1/folder3"),
                null, false, false, false, false);
        assert !worksheet.isPresent();

        worksheet = defaultWorksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/folder1/folder3/folder5"),
                null, false, true, true, true);
        assert worksheet.isPresent();
        assert worksheet.get().getPath().equals(new Path("/Worksheets/folder1/folder3/folder5"));
        assert worksheet.get().getId() == null;
        assert CollectionUtils.isEmpty(worksheet.get().getSubWorksheets());
        assert CollectionUtils.isEmpty(worksheet.get().getSameParentAtPrevLevelWorksheets());

        worksheet = defaultWorksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/folder3/"),
                "", false, true, true, true);
        assert worksheet.isPresent();
        assert worksheet.get().getPath().equals(new Path("/Worksheets/folder3/"));
        assert worksheet.get().getId() == null;
        assert CollectionUtils.isEqualCollection(worksheet.get().getSubWorksheets(),
                new HashSet<>(Arrays.asList(w5)));
        assert CollectionUtils.isEqualCollection(worksheet.get().getSameParentAtPrevLevelWorksheets(),
                new HashSet<>(Arrays.asList(w1, w2, w3, w4, w6)));

        worksheet = defaultWorksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/folder1/"),
                null, false, false, true, false);
        assertWorksheetWith(worksheet, w1);
        assert CollectionUtils.isEqualCollection(worksheet.get().getSubWorksheets(),
                new HashSet<>(Arrays.asList(w2, w3, w4)));
        assert CollectionUtils.isEmpty(worksheet.get().getSameParentAtPrevLevelWorksheets());

        worksheet = defaultWorksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/folder1/"),
                null, true, false, true, true);
        assertWorksheetWith(worksheet, w1);
        assert CollectionUtils.isEqualCollection(worksheet.get().getSubWorksheets(),
                new HashSet<>(Arrays.asList(w2, w3, w4)));
        assert CollectionUtils.isEqualCollection(worksheet.get().getSameParentAtPrevLevelWorksheets(),
                new HashSet<>(Arrays.asList(w5, w6)));

        // add nameLike
        worksheet = defaultWorksheetRepository.findByProjectIdAndPath(projectId,
                null,
                "der3", false, true, true, true);
        assert worksheet.isPresent();
        assert worksheet.get().getPath().equals(Path.root());
        assert worksheet.get().getId() == null;
        assert CollectionUtils.isEqualCollection(worksheet.get().getSubWorksheets(),
                new HashSet<>(Arrays.asList(w5)));
        assert CollectionUtils.isEmpty(worksheet.get().getSameParentAtPrevLevelWorksheets());

        // add nameLike
        worksheet = defaultWorksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/"),
                "de", false, true, true, true);
        assert worksheet.isPresent();
        assert worksheet.get().getPath().equals(Path.worksheets());
        assert worksheet.get().getId() == null;
        assert CollectionUtils.isEqualCollection(worksheet.get().getSubWorksheets(),
                new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5)));
        assert CollectionUtils.isEmpty(worksheet.get().getSameParentAtPrevLevelWorksheets());
    }

    private static void assertWorksheetWith(Optional<Worksheet> worksheet, Worksheet w1) {
        assert worksheet.isPresent();
        assert worksheet.get().getPath().equals(w1.getPath());
        assert worksheet.get().getId() > 0;
        assert worksheet.get().getProjectId().equals(w1.getProjectId());
        assert worksheet.get().getCreatorId().equals(w1.getCreatorId());
        assert worksheet.get().getVersion().equals(w1.getVersion());
        assert worksheet.get().getObjectId().equals(w1.getObjectId());
    }

    @Test
    @Transactional
    public void testListWithSubsByProjectIdAndPath() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        defaultWorksheetRepository.batchAdd(subWorksheets);

        assertThrows(BadArgumentException.class,
                () -> defaultWorksheetRepository.listWithSubsByProjectIdAndPath(projectId, null));
        List<Worksheet> result =
                defaultWorksheetRepository.listWithSubsByProjectIdAndPath(projectId, Path.root());
        assertEquals(result.size(), 6);
        assertEquals(new HashSet<>(result), new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6)));

        result =
                defaultWorksheetRepository.listWithSubsByProjectIdAndPath(projectId, Path.worksheets());
        assertEquals(result.size(), 6);
        assertEquals(new HashSet<>(result), new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6)));

        result =
                defaultWorksheetRepository.listWithSubsByProjectIdAndPath(projectId, new Path("/Worksheets/folder1/"));
        assertEquals(result.size(), 4);
        assertEquals(new HashSet<>(result), new HashSet<>(Arrays.asList(w1, w2, w3, w4)));

        result =
                defaultWorksheetRepository.listWithSubsByProjectIdAndPath(projectId, Path.repos());
        assertEquals(result.size(), 0);
    }

    @Test
    @Transactional
    public void testBatchAdd() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        defaultWorksheetRepository.batchAdd(subWorksheets);
        for (Worksheet worksheet : subWorksheets) {
            assertTrue(metadataRepository.findById(worksheet.getId()).isPresent());
        }
    }

    @Test
    @Transactional
    public void testBatchDelete() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        defaultWorksheetRepository.batchAdd(subWorksheets);

        defaultWorksheetRepository
                .batchDelete(subWorksheets.stream().map(Worksheet::getId).collect(Collectors.toSet()));

        for (Worksheet worksheet : subWorksheets) {
            assertFalse(metadataRepository.findById(worksheet.getId()).isPresent());
        }
    }

    @Test
    @Transactional
    public void testBatchUpdateById() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        defaultWorksheetRepository.batchAdd(subWorksheets);
        String objectId1 = id++ + "";
        String objectId2 = id++ + "";
        w1.setObjectId(objectId1);
        w2.setObjectId(objectId2);

        defaultWorksheetRepository.batchUpdateById(new HashSet<>(Arrays.asList(w1, w2)));

        assertEquals(metadataRepository.findById(w1.getId()).get().getObjectId(), objectId1);
        assertEquals(metadataRepository.findById(w2.getId()).get().getObjectId(), objectId2);
    }
}
