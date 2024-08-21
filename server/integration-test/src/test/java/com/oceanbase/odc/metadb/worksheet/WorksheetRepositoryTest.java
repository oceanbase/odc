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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
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
public class WorksheetRepositoryTest {

    @Autowired
    private WorksheetRepository worksheetRepository;

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
    public void findWithSubListByProjectIdAndPathAndNameLike() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        worksheetRepository.batchAdd(subWorksheets);
        Worksheet worksheet = worksheetRepository.findWithSubListByProjectIdAndPathAndNameLike(projectId,
                new Path("/Worksheets/folder1/folder3"),
                null);
        assert worksheet.isTemp();

        worksheet = worksheetRepository.findWithSubListByProjectIdAndPathAndNameLike(projectId,
                new Path("/Worksheets/folder3/"),
                "");
        assert worksheet.isTemp();
        assert worksheet.getPath().equals(new Path("/Worksheets/folder3/"));
        assert worksheet.getId() == null;
        assert CollectionUtils.isEqualCollection(worksheet.getSubWorksheets(),
                new HashSet<>(Arrays.asList(w5)));

        worksheet = worksheetRepository.findWithSubListByProjectIdAndPathAndNameLike(projectId,
                new Path("/Worksheets/folder1/"),
                null);
        assertWorksheetWith(worksheet, w1);
        assert CollectionUtils.isEqualCollection(worksheet.getSubWorksheets(),
                new HashSet<>(Arrays.asList(w2, w3, w4)));

        // add nameLike
        worksheet = worksheetRepository.findWithSubListByProjectIdAndPathAndNameLike(projectId,
                Path.root(),
                "der3");
        assert worksheet.isTemp();
        assert worksheet.getPath().equals(Path.root());
        assert worksheet.getId() == null;
        assert CollectionUtils.isEqualCollection(worksheet.getSubWorksheets(),
                new HashSet<>(Collections.singletonList(w5)));

        // add nameLike
        worksheet = worksheetRepository.findWithSubListByProjectIdAndPathAndNameLike(projectId,
                new Path("/Worksheets/folder1/"),
                "de");
        assertWorksheetWith(worksheet, w1);
        assert CollectionUtils.isEqualCollection(worksheet.getSubWorksheets(),
                new HashSet<>(Arrays.asList(w2, w3, w4)));

    }

    @Test
    @Transactional
    public void findWithSubListAndSameDirectParentListByProjectIdAndPathWithLock() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        worksheetRepository.batchAdd(subWorksheets);
        Worksheet worksheet =
                worksheetRepository.findWithSubListAndSameDirectParentListByProjectIdAndPathWithLock(projectId,
                        new Path("/Worksheets/folder1/folder3"));
        assert worksheet.isTemp();


        worksheet = worksheetRepository.findWithSubListAndSameDirectParentListByProjectIdAndPathWithLock(projectId,
                new Path("/Worksheets/folder3/"));
        assert worksheet.isTemp();
        assert worksheet.getPath().equals(new Path("/Worksheets/folder3/"));
        assert worksheet.getId() == null;
        assert CollectionUtils.isEqualCollection(worksheet.getSubWorksheets(),
                new HashSet<>(Arrays.asList(w5)));

        worksheet = worksheetRepository.findWithSubListAndSameDirectParentListByProjectIdAndPathWithLock(projectId,
                new Path("/Worksheets/folder1/"));
        assertWorksheetWith(worksheet, w1);
        assert CollectionUtils.isEqualCollection(worksheet.getSubWorksheets(),
                new HashSet<>(Arrays.asList(w2, w3, w4)));
    }

    @Test
    @Transactional
    public void findByProjectIdAndPath() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        worksheetRepository.batchAdd(subWorksheets);
        Optional<Worksheet> worksheet = worksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/folder1/folder3"));
        assert !worksheet.isPresent();

        worksheet = worksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/folder3/"));
        assert !worksheet.isPresent();

        worksheet = worksheetRepository.findByProjectIdAndPath(projectId,
                new Path("/Worksheets/folder1/"));
        assertWorksheetWith(worksheet.get(), w1);
        assert CollectionUtils.isEmpty(worksheet.get().getSubWorksheets());
    }

    private static void assertWorksheetWith(Worksheet worksheet, Worksheet w1) {
        assert !worksheet.isTemp();
        assert worksheet.getPath().equals(w1.getPath());
        assert worksheet.getId() > 0;
        assert worksheet.getProjectId().equals(w1.getProjectId());
        assert worksheet.getCreatorId().equals(w1.getCreatorId());
        assert worksheet.getVersion().equals(w1.getVersion());
        assert worksheet.getObjectId().equals(w1.getObjectId());
    }

    @Test
    @Transactional
    public void listByProjectIdAndInPaths() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        worksheetRepository.batchAdd(subWorksheets);
        List<Worksheet> worksheets = worksheetRepository.listByProjectIdAndInPaths(projectId,
                Arrays.asList(w1.getPath(), w2.getPath(), w6.getPath(), new Path("/Worksheets/folder3/")));
        assert worksheets.size() == 3;
        assertEquals(new HashSet<>(Arrays.asList(w1, w2, w6)), new HashSet<>(worksheets));
    }

    @Test
    @Transactional
    public void listWithSubListByProjectIdAndPath() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        worksheetRepository.batchAdd(subWorksheets);

        assertThrows(BadArgumentException.class,
                () -> worksheetRepository.listWithSubListByProjectIdAndPath(projectId, null));
        List<Worksheet> result =
                worksheetRepository.listWithSubListByProjectIdAndPath(projectId, Path.root());
        assertEquals(result.size(), 6);
        assertEquals(new HashSet<>(result), new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6)));

        result =
                worksheetRepository.listWithSubListByProjectIdAndPath(projectId, Path.worksheets());
        assertEquals(result.size(), 6);
        assertEquals(new HashSet<>(result), new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6)));

        result =
                worksheetRepository.listWithSubListByProjectIdAndPath(projectId, new Path("/Worksheets/folder1/"));
        assertEquals(result.size(), 4);
        assertEquals(new HashSet<>(result), new HashSet<>(Arrays.asList(w1, w2, w3, w4)));

        result =
                worksheetRepository.listWithSubListByProjectIdAndPath(projectId, Path.repos());
        assertEquals(result.size(), 0);
    }

    @Test
    @Transactional
    public void batchAdd() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        worksheetRepository.batchAdd(subWorksheets);
        for (Worksheet worksheet : subWorksheets) {
            assertTrue(metadataRepository.findById(worksheet.getId()).isPresent());
        }
    }

    @Test
    @Transactional
    public void batchDelete() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        worksheetRepository.batchAdd(subWorksheets);

        worksheetRepository
                .batchDelete(subWorksheets.stream().map(Worksheet::getId).collect(Collectors.toSet()));

        for (Worksheet worksheet : subWorksheets) {
            assertFalse(metadataRepository.findById(worksheet.getId()).isPresent());
        }
    }

    @Test
    @Transactional
    public void batchUpdateById() {
        Worksheet w1 = newWorksheet("/Worksheets/folder1/");
        Worksheet w2 = newWorksheet("/Worksheets/folder1/file2.sql");
        Worksheet w3 = newWorksheet("/Worksheets/folder1/folder4/");
        Worksheet w4 = newWorksheet("/Worksheets/folder1/folder4/file5.sql");
        Worksheet w5 = newWorksheet("/Worksheets/folder3/file3.sql");
        Worksheet w6 = newWorksheet("/Worksheets/file1.sql");
        Set<Worksheet> subWorksheets = new HashSet<>(Arrays.asList(w1, w2, w3, w4, w5, w6));
        worksheetRepository.batchAdd(subWorksheets);
        String objectId1 = id++ + "";
        String objectId2 = id++ + "";
        w1.setObjectId(objectId1);
        w2.setObjectId(objectId2);

        worksheetRepository.batchUpdateById(new HashSet<>(Arrays.asList(w1, w2)));

        assertEquals(metadataRepository.findById(w1.getId()).get().getObjectId(), objectId1);
        assertEquals(metadataRepository.findById(w2.getId()).get().getObjectId(), objectId2);
    }
}
